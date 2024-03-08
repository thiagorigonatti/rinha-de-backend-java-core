package me.thiagorigonatti.rinhadebackendjavacore;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionCRUD {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionCRUD.class);

    private TransactionCRUD() {
        throw new AssertionError();
    }

    public static void createTransactionTable() {

        String sql = """
                CREATE TABLE IF NOT EXISTS tb_transaction (
                                id           BIGSERIAL   PRIMARY KEY,
                                valor        BIGINT      NOT NULL,
                                tipo         "char"      NOT NULL,
                                descricao    VARCHAR(10) NOT NULL,
                                realizada_em TIMESTAMPTZ NOT NULL,
                                client_id    BIGINT      NOT NULL
                                );""";

        try (Connection connection = ConnectionFactory.getConn();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.execute();

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }


    public synchronized static JSONObject altInsert(Transaction transaction) {

        String sql1 = "SELECT * FROM tb_client WHERE id = ? FOR UPDATE;";
        LOG.info(sql1);

        String sql2 = "INSERT INTO tb_transaction (valor, tipo, descricao, client_id, realizada_em) VALUES (?, ?, ?, ?, now());";
        LOG.info(sql2);

        String sql3 = "UPDATE tb_client SET saldo = ? WHERE id = ?;";
        LOG.info(sql3);

        try (Connection connection = ConnectionFactory.getConn()) {

            connection.setAutoCommit(false);

            try (PreparedStatement selectClientStmt = connection.prepareStatement(sql1);
                 PreparedStatement insertTransactiontStmt = connection.prepareStatement(sql2);
                 PreparedStatement updateClientStmt = connection.prepareStatement(sql3, Statement.RETURN_GENERATED_KEYS)) {

                selectClientStmt.setLong(1, transaction.getClientId());
                insertTransactiontStmt.setLong(1, transaction.getValor());
                insertTransactiontStmt.setString(2, String.valueOf(transaction.getTipo()));
                insertTransactiontStmt.setString(3, transaction.getDescricao());
                insertTransactiontStmt.setLong(4, transaction.getClientId());

                ResultSet resultSet = selectClientStmt.executeQuery();

                if (resultSet.next()) {
                    final long limite = resultSet.getInt("limite");
                    final long saldo = resultSet.getInt("saldo");
                    final long novoSaldo;

                    if (transaction.getTipo().equals('d')) {
                        final long credit = limite - saldo * -1;
                        if (credit < transaction.getValor()) {
                            connection.rollback();
                            return null;
                        } else novoSaldo = saldo - transaction.getValor();

                    } else novoSaldo = saldo + transaction.getValor();

                    updateClientStmt.setLong(1, novoSaldo);
                    updateClientStmt.setLong(2, transaction.getClientId());
                    updateClientStmt.executeUpdate();
                    insertTransactiontStmt.executeUpdate();

                    ResultSet generatedKeys = updateClientStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.accumulate("limite", generatedKeys.getInt(2));
                        jsonObject.accumulate("saldo", generatedKeys.getInt(3));
                        connection.commit();
                        return jsonObject;
                    } else {
                        connection.rollback();
                        return null;
                    }
                }

            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
                try {
                    connection.rollback();
                } catch (SQLException sqlException2) {
                    sqlException2.printStackTrace();
                }
            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }

        return null;
    }

    public static JSONObject findTenById(long clientId) {

        String sql1 = "SELECT * FROM tb_client WHERE id = ? FOR UPDATE;";


        String sql2 = "SELECT * FROM tb_transaction WHERE client_id = ? ORDER BY realizada_em DESC LIMIT 10 FOR UPDATE;";

        LOG.info(sql1);

        LOG.info(sql2);

        try (Connection connection = ConnectionFactory.getConn()) {

            connection.setAutoCommit(false);

            try (PreparedStatement selectClientStmt = connection.prepareStatement(sql1);
                 PreparedStatement selectTransactionsStmt = connection.prepareStatement(sql2)) {


                selectClientStmt.setLong(1, clientId);
                selectTransactionsStmt.setLong(1, clientId);
                ResultSet ClientResultSet = selectClientStmt.executeQuery();
                ResultSet transactionsResultSet = selectTransactionsStmt.executeQuery();
                List<Transaction> transactionList = new ArrayList<>();

                while (transactionsResultSet.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setId(transactionsResultSet.getLong("id"));
                    transaction.setValor(transactionsResultSet.getLong("valor"));
                    transaction.setTipo(transactionsResultSet.getString("tipo").toCharArray()[0]);
                    transaction.setDescricao(transactionsResultSet.getString("descricao"));
                    transaction.setClientId(clientId);
                    transactionList.add(transaction);
                }

                if (ClientResultSet.next()) {
                    JSONObject saldo = new JSONObject();
                    saldo.accumulate("total", ClientResultSet.getInt("saldo"));
                    saldo.accumulate("limite", ClientResultSet.getInt("limite"));
                    saldo.accumulate("data_extrato", Instant.now());
                    JSONObject saldoObj = new JSONObject();
                    saldoObj.accumulate("saldo", saldo);
                    saldoObj.accumulate("ultimas_transacoes", transactionList);
                    connection.commit();
                    return saldoObj;
                }

            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
                try {
                    connection.rollback();
                } catch (SQLException sqlException2) {
                    sqlException2.printStackTrace();
                }
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }

        return null;
    }
}