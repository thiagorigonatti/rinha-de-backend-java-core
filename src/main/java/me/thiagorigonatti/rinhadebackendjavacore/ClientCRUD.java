package me.thiagorigonatti.rinhadebackendjavacore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.*;

public class ClientCRUD {

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


    public static JsonNode insert(JsonNode clientNode) {

        String sql = "INSERT INTO tb_client (limite, saldo) VALUES (?, ?);";


        try (Connection connection = ConnectionFactory.getConn();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setLong(1, clientNode.get("limite").asLong());
            preparedStatement.setLong(2, clientNode.get("saldo").asLong());

            preparedStatement.executeUpdate();
            ResultSet generatedKey = preparedStatement.getGeneratedKeys();

            if (generatedKey.next()) {
                ObjectNode objectNode = JSONUtils.OBJECT_MAPPER.createObjectNode();
                objectNode.put("id", generatedKey.getLong("id"));
                objectNode.put("limite", generatedKey.getLong("limite"));
                objectNode.put("saldo", generatedKey.getLong("saldo"));
                return objectNode;
            } else {
                return null;
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return null;
    }


    public static JsonNode findById(long clientId) {

        String sql = "SELECT * FROM tb_client WHERE id = ? FOR UPDATE;";


        try (Connection connection = ConnectionFactory.getConn();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, clientId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {

                ObjectNode objectNode = JSONUtils.OBJECT_MAPPER.createObjectNode();
                objectNode.put("id", resultSet.getLong("id"));
                objectNode.put("limite", resultSet.getLong("limite"));
                objectNode.put("saldo", resultSet.getLong("saldo"));
                return objectNode;
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }

        return null;
    }
}
