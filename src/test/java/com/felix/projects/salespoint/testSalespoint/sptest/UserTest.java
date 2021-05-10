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
public class UserTest extends TestNGCitrusTestRunner {

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
  public void testGetAllUsers() {
    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .get("/users")
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
  public void testGetUserById() {

    String selectQuery = "select * from users limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      int id = resultSet.getInt("id");
      String name = resultSet.getString("name");
      String password = resultSet.getString("password");
      String email = resultSet.getString("email");
      int role = resultSet.getInt("role");
      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .get("/users/" + id)
                  .accept(MediaType.APPLICATION_JSON_VALUE));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.OK)
                  .messageType(MessageType.JSON)
                  .validate("$.name", name)
                  .validate("$.password", password)
                  .validate("$.email", email)
                  .validate("$.role", role));

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testGetBadUserById() {

    int id = -1;

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .get("/users/" + id)
                .accept(MediaType.APPLICATION_JSON_VALUE));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.NOT_FOUND)
                .validate("$.message", "User " + id + " not found"));
  }

  @Test
  @CitrusTest
  public void testCreateUser(@CitrusResource @Optional TestContext testContext)
      throws JSONException {
    JSONObject jsonObject = new JSONObject();

    String name = "Odin";
    String password = "AllF4ther$11";
    String email = "odin@gmail.com";
    Integer role = 0;

    jsonObject.put("name", name);
    jsonObject.put("password", password);
    jsonObject.put("email", email);
    jsonObject.put("role", role);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/users")
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
                .validate("$.password", password)
                .validate("$.email", email)
                .validate("$.role", role));

    String id = testContext.getVariable("id");

    String query = "delete from users where id = " + id;

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(query); ) {
      selectPreparedStatement.executeUpdate();
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testCreateUserBadName() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    String name = "0din";
    String password = "AllF4ther$11";
    String email = "odin@gmail.com";
    Integer role = 0;

    jsonObject.put("name", name);
    jsonObject.put("password", password);
    jsonObject.put("email", email);
    jsonObject.put("role", role);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.BAD_REQUEST)
                .validate("$.subErrors.[0].message", "Must only be letters."));
  }

  @Test
  @CitrusTest
  public void testCreateUserBadPassword() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    String name = "Odin";
    String password = "testtesttest";
    String email = "odin@gmail.com";
    Integer role = 0;

    jsonObject.put("name", name);
    jsonObject.put("password", password);
    jsonObject.put("email", email);
    jsonObject.put("role", role);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.BAD_REQUEST)
                .validate(
                    "$.subErrors.[0].message",
                    "Must contain : At least a capital letter, at least a minuscule letter, at least a number and at least a symbol."));
  }

  @Test
  @CitrusTest
  public void testCreateUserBadEmail() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    String name = "Odin";
    String password = "AllF4ther$11";
    String email = "test";
    Integer role = 0;

    jsonObject.put("name", name);
    jsonObject.put("password", password);
    jsonObject.put("email", email);
    jsonObject.put("role", role);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.BAD_REQUEST)
                .validate("$.subErrors.[0].message", "This is not a valid email address."));
  }

  @Test
  @CitrusTest
  public void testCreateUserBadRole() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    String name = "Odin";
    String password = "AllF4ther$11";
    String email = "odin@gmail.com";
    Integer role = 4;

    jsonObject.put("name", name);
    jsonObject.put("password", password);
    jsonObject.put("email", email);
    jsonObject.put("role", role);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.NOT_ACCEPTABLE)
                .validate("$.message", "Error creating the user : Role invalid."));
  }

  @Test
  @CitrusTest
  public void testUpdateUser() throws JSONException {
    String selectQuery = "select * from users limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      int id = resultSet.getInt("id");
      String oldName = resultSet.getString("name");

      JSONObject jsonObject = new JSONObject();

      String newName = "Gwyn";

      jsonObject.put("name", newName);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .put("/users/" + id)
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

      String query = "update users set name = '" + oldName + "' where id = " + id;

      PreparedStatement updateNamePreparedStatement = connection.prepareStatement(query);
      updateNamePreparedStatement.executeUpdate();

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testDeleteUser() {

    String selectQuery = "select * from users limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      int id = resultSet.getInt("id");
      String name = resultSet.getString("name");
      String password = resultSet.getString("password");
      String email = resultSet.getString("email");
      int role = resultSet.getInt("role");

      String wishlistQuery = "select * from wishlist where userid = " + id;
      PreparedStatement wishlistPreparedStatement = connection.prepareStatement(wishlistQuery);
      ResultSet wishlistList = wishlistPreparedStatement.executeQuery();

      List<Integer> items = new ArrayList<>();

      while (wishlistList.next()) {
        items.add(wishlistList.getInt("itemid"));
      }

      String itemQuery = "select * from items where \"owner\" =" + id;
      PreparedStatement itemPreparedStatement = connection.prepareStatement(itemQuery);
      ResultSet ownedStatement = itemPreparedStatement.executeQuery();

      List<Integer> listOfIds = new ArrayList<>();
      List<String> listOfNames = new ArrayList<>();
      List<String> listOfDescriptions = new ArrayList<>();
      List<Integer> listOfStock = new ArrayList<>();
      List<Float> listOfPrices = new ArrayList<>();

      while (ownedStatement.next()) {
        listOfIds.add(ownedStatement.getInt("id"));
        listOfNames.add(ownedStatement.getString("name"));
        listOfDescriptions.add(ownedStatement.getString("description"));
        listOfStock.add(ownedStatement.getInt("stock"));
        listOfPrices.add(ownedStatement.getFloat("price"));
      }

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .delete("/users/" + id)
                  .contentType(MediaType.APPLICATION_JSON_VALUE));

      http(
          httpActionBuilder ->
              httpActionBuilder.client(httpClient).receive().response(HttpStatus.NO_CONTENT));

      String insertQuery =
          "insert into users (id, \"name\", \"password\", email, role) values ("
              + id
              + ", "
              + "'"
              + name
              + "', "
              + "'"
              + password
              + "', "
              + "'"
              + email
              + "', "
              + role
              + ")";

      PreparedStatement insertPreparedStatement = connection.prepareStatement(insertQuery);
      insertPreparedStatement.executeUpdate();

      if (listOfIds.size() > 0) {
        for (int i = 0; i < listOfIds.size(); i++) {
          String insertItemQuery =
              "insert into items (id, \"name\", description, stock, price, \"owner\") values ("
                  + listOfIds.get(i)
                  + ", "
                  + "'"
                  + listOfNames.get(i)
                  + "', "
                  + "'"
                  + listOfDescriptions.get(i)
                  + "', "
                  + listOfStock.get(i)
                  + ", "
                  + listOfPrices.get(i)
                  + ", "
                  + id
                  + ")";

          PreparedStatement insertItemStatement = connection.prepareStatement(insertItemQuery);
          insertItemStatement.executeUpdate();
        }
      }

      if (items.size() > 0) {
        for (int itemId : items) {
          String insertWishListQuery =
              "insert into wishlist (userid, itemid) values (" + id + "," + itemId + ")";
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
  public void testDeleteUserDoesNotExist() {
    int id = -1;
    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .delete("/users/" + id)
                .contentType(MediaType.APPLICATION_JSON_VALUE));

    http(
        httpActionBuilder ->
            httpActionBuilder.client(httpClient).receive().response(HttpStatus.NOT_FOUND));
  }
}
