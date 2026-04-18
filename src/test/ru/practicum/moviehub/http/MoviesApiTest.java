package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        gson = new Gson();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMoviesWhenNotEmptyReturnsArrayAllMovies() throws Exception {
        moviesStore.save(new Movie("Интерстеллар", 2014));
        moviesStore.save(new Movie("Побег из Шоушенка", 1994));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals("Интерстеллар", movies.get(0).getTitle());
        assertEquals(2014, movies.get(0).getYear());
        assertEquals("Побег из Шоушенка", movies.get(1).getTitle());
        assertEquals(1994, movies.get(1).getYear());
    }

    @Test
    void addMovieWhenDataIsValid() throws Exception {
        String movieJson = "{\"title\":\"Интерстеллар\",\"year\":2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
    }

    @Test
    void returnErrorWithEmptyTitle() throws Exception {
        String movieJson = "{\"title\":\"\",\"year\":2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertTrue(errorResponse.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    void returnErrorWithTitleMoreOneHundredSize() throws Exception {
        String x = "x";
        String longTitle = x.repeat(101);
        String movieJson = "{\"title\":\"" + longTitle + "\",\"year\":2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertTrue(errorResponse.getDetails().contains("название не должно быть длиннее 100 символов"));
    }

    @Test
    void returnErrorWithYearLess() throws Exception {
        int maxReleaseYear = Year.now().getValue() + 1;
        String movieJson = "{\"title\":\"Интерстеллар\",\"year\":1887}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertTrue(resp.body().contains("год должен быть между 1888 и " + maxReleaseYear));
    }

    @Test
    void returnErrorWithYearMore() throws Exception {
        int maxReleaseYear = Year.now().getValue() + 1;
        int maxReleaseYearPlusOne = maxReleaseYear + 1;
        String movieJson = "{\"title\":\"Интерстеллар\",\"year\":" + maxReleaseYearPlusOne + "}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertTrue(resp.body().contains("год должен быть между 1888 и " + maxReleaseYear));
    }

    @Test
    void returnErrorWithWrongContentType() throws Exception {
        String movieJson = "{\"title\":\"Интерстеллар\",\"year\":2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415 Unsupported Media Type");
    }

    @Test
    void returnErrorWithBrokenJson() throws Exception {
        String brokenJson = "{\"title\":\"Интерстеллар\",\"year\":2014";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(brokenJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode(), "Сервер должен вернуть 400 на битый JSON");
    }

    @Test
    void getMovieByValidId() throws Exception {
        moviesStore.save(new Movie("Интерстеллар", 2014));
        moviesStore.save(new Movie("Побег из Шоушенка", 1994));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/2 должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        Movie movie = gson.fromJson(body, Movie.class);
        assertEquals("Побег из Шоушенка", movie.getTitle());
        assertEquals(1994, movie.getYear());
    }

    @Test
    void getReturnErrorIfMovieNotFound() throws Exception {
        moviesStore.save(new Movie("Интерстеллар", 2014));
        moviesStore.save(new Movie("Побег из Шоушенка", 1994));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/3 должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void getReturnErrorIfIdIsNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/notnumber"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/notnumber должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void removeMovieById() throws Exception {
        moviesStore.save(new Movie("Интерстеллар", 2014));
        moviesStore.save(new Movie("Побег из Шоушенка", 1994));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE /movies/1 должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    @Test
    void deleteReturnErrorIfMovieNotFound() throws Exception {
        moviesStore.save(new Movie("Интерстеллар", 2014));
        moviesStore.save(new Movie("Побег из Шоушенка", 1994));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE /movies/3 должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    @Test
    void deleteReturnErrorIfIdIsNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/notnumber"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/notnumber должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void returnMoviesOfTheYear() throws Exception {
        moviesStore.save(new Movie("Интерстеллар", 2014));
        moviesStore.save(new Movie("Побег из Шоушенка", 1994));
        moviesStore.save(new Movie("Стражи галактики", 2014));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2014"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=2014 должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals("Интерстеллар", movies.get(0).getTitle());
        assertEquals(2014, movies.get(0).getYear());
        assertEquals("Стражи галактики", movies.get(1).getTitle());
        assertEquals(2014, movies.get(1).getYear());
    }

    @Test
    void returnEmptyArrayIfThereAreNoMoviesWithYear() throws Exception {
        moviesStore.save(new Movie("Интерстеллар", 2014));
        moviesStore.save(new Movie("Побег из Шоушенка", 1994));
        moviesStore.save(new Movie("Стражи галактики", 2014));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1993"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=1993 должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void returnErrorIfYearIsNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=xxxx"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies?year=xxxx должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(resp.body().contains("Некорректный параметр запроса — 'year'"));
    }
}