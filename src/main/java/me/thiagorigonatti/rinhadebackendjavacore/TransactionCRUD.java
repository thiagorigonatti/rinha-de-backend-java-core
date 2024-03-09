package me.thiagorigonatti.rinhadebackendjavacore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;

public class TransactionCRUD {

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


    public synchronized static JsonNode insert(JsonNode transactionNode, long clientId) {

        String sql1 = "SELECT * FROM tb_client WHERE id = ? FOR UPDATE;";

        String sql2 = "INSERT INTO tb_transaction (valor, tipo, descricao, client_id, realizada_em) VALUES (?, ?, ?, ?, now());";

        String sql3 = "UPDATE tb_client SET saldo = ? WHERE id = ?;";

        try (Connection connection = ConnectionFactory.getConn()) {

            connection.setAutoCommit(false);

            try (PreparedStatement selectClientStmt = connection.prepareStatement(sql1);
                 PreparedStatement insertTransactiontStmt = connection.prepareStatement(sql2);
                 PreparedStatement updateClientStmt = connection.prepareStatement(sql3, Statement.RETURN_GENERATED_KEYS)) {

                selectClientStmt.setLong(1, clientId);

                insertTransactiontStmt.setLong(1, transactionNode.get("valor").asLong());
                insertTransactiontStmt.setString(2, transactionNode.get("tipo").asText());
                insertTransactiontStmt.setString(3, transactionNode.get("descricao").asText());
                insertTransactiontStmt.setLong(4, clientId);

                ResultSet resultSet = selectClientStmt.executeQuery();

                if (resultSet.next()) {
                    final long limite = resultSet.getLong("limite");
                    final long saldo = resultSet.getLong("saldo");
                    final long novoSaldo;

                    if (transactionNode.get("tipo").asText().equals("d")) {
                        final long credit = limite - saldo * -1;
                        if (credit < transactionNode.get("valor").asLong()) {
                            connection.rollback();
                            return null;
                        } else novoSaldo = saldo - transactionNode.get("valor").asLong();

                    } else novoSaldo = saldo + transactionNode.get("valor").asLong();

                    updateClientStmt.setLong(1, novoSaldo);
                    updateClientStmt.setLong(2, clientId);
                    updateClientStmt.executeUpdate();
                    insertTransactiontStmt.executeUpdate();

                    ResultSet generatedKeys = updateClientStmt.getGeneratedKeys();

                    if (generatedKeys.next()) {
                        ObjectNode objectNode = JSONUtils.OBJECT_MAPPER.createObjectNode();
                        objectNode.put("limite", generatedKeys.getLong(2));
                        objectNode.put("saldo", generatedKeys.getLong(3));
                        connection.commit();
                        return objectNode;

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

    public static JsonNode findTenById(long clientId) {

        String sql1 = "SELECT * FROM tb_client WHERE id = ? FOR UPDATE;";

        String sql2 = "SELECT * FROM tb_transaction WHERE client_id = ? ORDER BY realizada_em DESC LIMIT 10 FOR UPDATE;";

        try (Connection connection = ConnectionFactory.getConn()) {

            connection.setAutoCommit(false);

            try (PreparedStatement selectClientStmt = connection.prepareStatement(sql1);
                 PreparedStatement selectTransactionsStmt = connection.prepareStatement(sql2)) {

                selectClientStmt.setLong(1, clientId);
                selectTransactionsStmt.setLong(1, clientId);
                ResultSet clientResultSet = selectClientStmt.executeQuery();
                ResultSet transactionsResultSet = selectTransactionsStmt.executeQuery();
                ArrayNode arrayNode = JSONUtils.OBJECT_MAPPER.createArrayNode();

                while (transactionsResultSet.next()) {
                    ObjectNode objectNode = JSONUtils.OBJECT_MAPPER.createObjectNode();
                    objectNode.put("valor", transactionsResultSet.getLong("valor"));
                    objectNode.put("tipo", transactionsResultSet.getString("tipo"));
                    objectNode.put("descricao", transactionsResultSet.getString("descricao"));
                    objectNode.put("realizada_em", transactionsResultSet
                            .getTimestamp("realizada_em").toInstant()
                            .atZone(ZoneId.of("America/Sao_Paulo")).toString());

                    arrayNode.add(objectNode);
                }

                if (clientResultSet.next()) {

                    ObjectNode saldo = JSONUtils.OBJECT_MAPPER.createObjectNode()
                            .put("total", clientResultSet.getLong("saldo"))
                            .put("data_extrato", Instant.now().atZone(ZoneId.of("America/Sao_Paulo")).toString())
                            .put("limite", clientResultSet.getLong("limite"));


                    ObjectNode extrato = JSONUtils.OBJECT_MAPPER.createObjectNode();

                    extrato.set("saldo", saldo);
                    extrato.set("ultimas_transacoes", arrayNode);

                    connection.commit();
                    return extrato;
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