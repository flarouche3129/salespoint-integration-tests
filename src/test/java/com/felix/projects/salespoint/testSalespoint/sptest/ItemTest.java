package com.felix.projects.salespoint.testSalespoint.sptest;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.message.MessageType;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Test
public class ItemTest extends TestNGCitrusTestRunner {

  @Autowired private HttpClient httpClient;

  private Connection connectToDB() throws SQLException {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
    dataSource.setUser("postgres");
    dataSource.setPassword("postgres");
    return dataSource.getConnection();
  }

  @Test
  @CitrusTest
  public void testGetAllItems() {

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .get("/items")
                .accept(MediaType.APPLICATION_JSON_VALUE));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.OK)
                .messageType(MessageType.JSON));
  }

  @Test
  @CitrusTest
  public void testGetItemById() {

    String selectQuery = "select * from items limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      int id = resultSet.getInt("id");
      String name = resultSet.getString("name");
      String description = resultSet.getString("description");
      int stock = resultSet.getInt("stock");
      float price = resultSet.getFloat("price");
      int owner = resultSet.getInt("owner");

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .get("/items/" + id)
                  .accept(MediaType.APPLICATION_JSON_VALUE));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.OK)
                  .messageType(MessageType.JSON)
                  .validate("$.name", name)
                  .validate("$.description", description)
                  .validate("$.stock", stock)
                  .validate("$.price", price)
                  .validate("$.owner", owner));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testGetBadItemById() {
    int id = -1;

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .get("/items/" + id)
                .accept(MediaType.APPLICATION_JSON_VALUE));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.NOT_FOUND)
                .validate("$.message", "Item " + id + " not found"));
  }

  @Test
  @CitrusTest
  public void testCreateItem(@CitrusResource @Optional TestContext testContext)
      throws JSONException {

    String ownerQuery = "select id from users limit 1";
    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(ownerQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      JSONObject jsonObject = new JSONObject();

      String name = "Moonlight Greatsword";
      String description = "A mega thicc sword!";
      int stock = 5;
      float price = 69.99f;
      int owner = resultSet.getInt("id");
      jsonObject.put("name", name);
      jsonObject.put("description", description);
      jsonObject.put("stock", stock);
      jsonObject.put("price", price);
      jsonObject.put("owner", owner);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/items")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.CREATED)
                  .messageType(MessageType.JSON)
                  .extractFromPayload("$.id", "id")
                  .validate("$.name", name)
                  .validate("$.description", description)
                  .validate("$.stock", stock)
                  .validate("$.price", price)
                  .validate("$.owner", owner));

      String id = testContext.getVariable("id");

      String query = "delete from items where id = " + id;
      PreparedStatement deletePreparedStatement = connection.prepareStatement(query);
      deletePreparedStatement.executeQuery();
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testCreateItemBadName() throws JSONException {

    String ownerQuery = "select id from users limit 1";
    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(ownerQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      JSONObject jsonObject = new JSONObject();

      String name = "Moonlight Greatsword****";
      String description = "A mega thicc sword!";
      int stock = 5;
      float price = 69.99f;
      int owner = resultSet.getInt("id");
      jsonObject.put("name", name);
      jsonObject.put("description", description);
      jsonObject.put("stock", stock);
      jsonObject.put("price", price);
      jsonObject.put("owner", owner);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/items")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.BAD_REQUEST)
                  .validate("$.message", "Validation error"));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testCreateItemBadDescription() throws JSONException {
    String ownerQuery = "select id from users limit 1";
    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(ownerQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      JSONObject jsonObject = new JSONObject();

      String name = "Moonlight Greatsword****";
      String description = "";
      int stock = 5;
      float price = 69.99f;
      int owner = resultSet.getInt("id");
      jsonObject.put("name", name);
      jsonObject.put("description", description);
      jsonObject.put("stock", stock);
      jsonObject.put("price", price);
      jsonObject.put("owner", owner);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/items")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.BAD_REQUEST)
                  .validate("$.message", "Validation error"));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testCreateItemBadStock() throws JSONException {
    String ownerQuery = "select id from users limit 1";
    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(ownerQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      JSONObject jsonObject = new JSONObject();

      String name = "Moonlight Greatsword****";
      String description = "A mega thicc sword!";
      int stock = -1;
      float price = 69.99f;
      int owner = resultSet.getInt("id");
      jsonObject.put("name", name);
      jsonObject.put("description", description);
      jsonObject.put("stock", stock);
      jsonObject.put("price", price);
      jsonObject.put("owner", owner);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/items")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.BAD_REQUEST)
                  .validate("$.message", "Validation error"));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testCreateItemBadPrice() throws JSONException {
    String ownerQuery = "select id from users limit 1";
    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(ownerQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      JSONObject jsonObject = new JSONObject();

      String name = "Moonlight Greatsword****";
      String description = "A mega thicc sword!";
      int stock = 5;
      float price = -1f;
      int owner = resultSet.getInt("id");
      jsonObject.put("name", name);
      jsonObject.put("description", description);
      jsonObject.put("stock", stock);
      jsonObject.put("price", price);
      jsonObject.put("owner", owner);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/items")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.BAD_REQUEST)
                  .validate("$.message", "Validation error"));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testCreateItemBadOwner() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    String name = "Moonlight Greatsword$$$";
    String description = "A mega thicc sword!";
    int stock = 5;
    float price = 69.99f;
    int owner = -1;

    jsonObject.put("name", name);
    jsonObject.put("description", description);
    jsonObject.put("stock", stock);
    jsonObject.put("price", price);
    jsonObject.put("owner", owner);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/items")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.NOT_ACCEPTABLE)
                .validate("$.message", "Error creating the item : Owner invalid."));
  }

  @Test
  @CitrusTest
  public void testUpdateItem() throws JSONException {

    String selectQuery = "select * from items limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      int id = resultSet.getInt("id");
      String oldName = resultSet.getString("name");

      JSONObject jsonObject = new JSONObject();

      String newName = "Moonlight Greatsword";

      jsonObject.put("name", newName);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .put("/items/" + id)
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.OK)
                  .messageType(MessageType.JSON)
                  .validate("$.name", newName));

      String query = "update items set name = '" + oldName + "' where id = " + id;

      PreparedStatement updateNamePreparedStatement = connection.prepareStatement(query);
      updateNamePreparedStatement.executeUpdate();

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testDeleteItem() {

    String selectQuery = "select * from items limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      int id = resultSet.getInt("id");
      String name = resultSet.getString("name");
      String description = resultSet.getString("description");
      int stock = resultSet.getInt("stock");
      float price = resultSet.getFloat("price");
      int owner = resultSet.getInt("owner");

      String wishlistQuery = "select * from wishlist where itemid = " + id;
      PreparedStatement wishlistPreparedStatement = connection.prepareStatement(wishlistQuery);
      ResultSet wishlistList = wishlistPreparedStatement.executeQuery();

      List<Integer> users = new ArrayList<>();

      while (wishlistList.next()) {
        users.add(wishlistList.getInt("userid"));
      }

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .delete("/items/" + id)
                  .contentType(MediaType.APPLICATION_JSON_VALUE));

      http(
          httpActionBuilder ->
              httpActionBuilder.client(httpClient).receive().response(HttpStatus.NO_CONTENT));

      String insertQuery =
          "insert into items (id, \"name\", description, stock, price, \"owner\") values ("
              + id
              + ", "
              + "'"
              + name
              + "', "
              + "'"
              + description
              + "', "
              + stock
              + ", "
              + price
              + ", "
              + owner
              + ")";

      PreparedStatement insertPreparedStatement = connection.prepareStatement(insertQuery);
      insertPreparedStatement.executeUpdate();

      if (users.size() > 0) {
        for (int userId : users) {
          String insertWishListQuery =
              "insert into wishlist (userid, itemid) values (" + userId + "," + id + ")";
          PreparedStatement insertWishListStatement =
              connection.prepareStatement(insertWishListQuery);
          insertWishListStatement.executeUpdate();
        }
      }

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testDeleteItemDoesNotExist() {

    int id = -1;
    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .delete("/items/" + id)
                .contentType(MediaType.APPLICATION_JSON_VALUE));

    http(
        httpActionBuilder ->
            httpActionBuilder.client(httpClient).receive().response(HttpStatus.NOT_FOUND));
  }
}
