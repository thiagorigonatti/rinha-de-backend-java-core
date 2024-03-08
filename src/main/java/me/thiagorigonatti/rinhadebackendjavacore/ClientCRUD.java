package me.thiagorigonatti.rinhadebackendjavacore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class ClientCRUD {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionCRUD.class);

    private ClientCRUD() {
        throw new AssertionError();
    }

    public static void createClientTable() {

        String sql = """
                CREATE TABLE IF NOT EXISTS tb_client (
                                id     BIGSERIAL PRIMARY KEY,
                                limite BIGINT NOT NULL,
                                saldo  BIGINT NOT NULL
                                );""";

        try (Connection connection = ConnectionFactory.getConn(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.execute();
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }


    public static Client insert(Client client) {

        String sql = "INSERT INTO tb_client (limite, saldo) VALUES (?, ?);";

        LOG.info(sql);

        try (Connection connection = ConnectionFactory.getConn(); PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setLong(1, client.getLimite());
            preparedStatement.setLong(2, client.getSaldo());

            preparedStatement.executeUpdate();
            ResultSet generatedKey = preparedStatement.getGeneratedKeys();

            if (generatedKey.next()) {
                client.setId(generatedKey.getLong("id"));
                return client;
            } else {
                return null;
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return null;
    }


    public static Client findById(long clientId) {

        String sql = "SELECT * FROM tb_client WHERE id = ? FOR UPDATE;";

        LOG.info(sql);

        try (Connection connection = ConnectionFactory.getConn(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, clientId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Client client = new Client();
                client.setId(clientId);
                client.setLimite(resultSet.getLong("limite"));
                client.setSaldo(resultSet.getLong("saldo"));
                return client;
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }

        return null;
    }
}
