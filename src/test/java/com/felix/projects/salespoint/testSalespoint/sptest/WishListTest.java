package com.felix.projects.salespoint.testSalespoint.sptest;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.message.MessageType;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Test
public class WishListTest extends TestNGCitrusTestRunner {
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
  public void testGetAllWishLists() {
    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .get("/wishlists")
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
  public void testGetAllItemsWishedByUser() {

    String selectQuery = "select userid from wishlist limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      int userId = resultSet.getInt("userid");

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .get("/wishlists/users/" + userId)
                  .accept(MediaType.APPLICATION_JSON_VALUE));
      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.OK)
                  .messageType(MessageType.JSON));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testGetAllUserWishingForItem() {
    String selectQuery = "select itemid from wishlist limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      int itemId = resultSet.getInt("itemid");

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .get("/wishlists/items/" + itemId)
                  .accept(MediaType.APPLICATION_JSON_VALUE));
      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.OK)
                  .messageType(MessageType.JSON));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testAddToWishList() throws JSONException {

    String userQuery = "select id from users where id not in (select userid from wishlist) limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectUserPreparedStatement = connection.prepareStatement(userQuery);
        ResultSet userFetched = selectUserPreparedStatement.executeQuery()) {

      userFetched.next();
      int userId = userFetched.getInt("id");

      String itemQuery =
          "select id from items where id not in (select itemid from wishlist) and \"owner\" != "
              + userId
              + " and stock != 0 limit 1";

      PreparedStatement selectItemPreparedStatement = connection.prepareStatement(itemQuery);
      ResultSet itemFetched = selectItemPreparedStatement.executeQuery();
      itemFetched.next();
      int itemId = itemFetched.getInt("id");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("item", itemId);
      jsonObject.put("user", userId);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/wishlists")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.CREATED)
                  .messageType(MessageType.JSON)
                  .validate("$.user", userId)
                  .validate("$.item", itemId));

      String deleteQuery =
          "delete from wishlist where userid = " + userId + " and itemid = " + itemId;

      PreparedStatement deletePreparedStatement = connection.prepareStatement(deleteQuery);
      deletePreparedStatement.executeUpdate();

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testAddToWishListBadItem() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    int itemId = 1;
    int userId = 55;

    jsonObject.put("item", itemId);
    jsonObject.put("user", userId);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/wishlists")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.NOT_ACCEPTABLE)
                .validate("$.message", "Error creating the wishlist."));
  }

  @Test
  @CitrusTest
  public void testAddToWishListBadUser() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    int itemId = 89;
    int userId = 1;

    jsonObject.put("item", itemId);
    jsonObject.put("user", userId);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .post("/wishlists")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .receive()
                .response(HttpStatus.NOT_ACCEPTABLE)
                .validate("$.message", "Error creating the wishlist."));
  }

  @Test
  @CitrusTest
  public void testAddToWishListEmptyStock() throws JSONException {
    String userQuery = "select id from users where id not in (select userid from wishlist) limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectUserPreparedStatement = connection.prepareStatement(userQuery);
        ResultSet userFetched = selectUserPreparedStatement.executeQuery()) {

      userFetched.next();
      int userId = userFetched.getInt("id");

      String itemQuery = "select id from items where stock = 0";

      PreparedStatement selectItemPreparedStatement = connection.prepareStatement(itemQuery);
      ResultSet itemFetched = selectItemPreparedStatement.executeQuery();
      itemFetched.next();
      int itemId = itemFetched.getInt("id");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("item", itemId);
      jsonObject.put("user", userId);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/wishlists")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.NOT_ACCEPTABLE)
                  .validate("$.message", "Error creating the wishlist."));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testAddToWishListAlreadyOwned() throws JSONException {
    String userQuery = "select id from users where id not in (select userid from wishlist) limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectUserPreparedStatement = connection.prepareStatement(userQuery);
        ResultSet userFetched = selectUserPreparedStatement.executeQuery()) {

      userFetched.next();
      int userId = userFetched.getInt("id");

      String itemQuery = "select id from items where \"owner\" = " + userId;

      PreparedStatement selectItemPreparedStatement = connection.prepareStatement(itemQuery);
      ResultSet itemFetched = selectItemPreparedStatement.executeQuery();
      itemFetched.next();
      int itemId = itemFetched.getInt("id");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("item", itemId);
      jsonObject.put("user", userId);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .post("/wishlists")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .receive()
                  .response(HttpStatus.NOT_ACCEPTABLE)
                  .validate("$.message", "Error creating the wishlist."));
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testRemoveFromWishList() {

    String selectQuery = "select * from wishlist limit 1";

    try (Connection connection = connectToDB();
        PreparedStatement selectPreparedStatement = connection.prepareStatement(selectQuery);
        ResultSet resultSet = selectPreparedStatement.executeQuery()) {

      resultSet.next();
      int userId = resultSet.getInt("userid");
      int itemId = resultSet.getInt("itemid");

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("itemId", itemId);
      jsonObject.put("userId", userId);

      http(
          httpActionBuilder ->
              httpActionBuilder
                  .client(httpClient)
                  .send()
                  .delete("/wishlists")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .payload(jsonObject.toString()));

      http(
          httpActionBuilder ->
              httpActionBuilder.client(httpClient).receive().response(HttpStatus.NO_CONTENT));

      String insertQuery =
          "insert into wishlist (userid, itemid) values (" + userId + ", " + itemId + ")";

      PreparedStatement insertPreparedStatement = connection.prepareStatement(insertQuery);
      insertPreparedStatement.executeQuery();

    } catch (SQLException | JSONException throwables) {
      throwables.printStackTrace();
    }
  }

  @Test
  @CitrusTest
  public void testRemoveWishListDoesNotExist() throws JSONException {

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("itemId", -1);
    jsonObject.put("userId", -1);

    http(
        httpActionBuilder ->
            httpActionBuilder
                .client(httpClient)
                .send()
                .delete("/wishlists")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .payload(jsonObject.toString()));

    http(
        httpActionBuilder ->
            httpActionBuilder.client(httpClient).receive().response(HttpStatus.NOT_FOUND));
  }
}
