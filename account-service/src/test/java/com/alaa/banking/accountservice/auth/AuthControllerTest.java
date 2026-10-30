package com.alaa.banking.accountservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.alaa.banking.accountservice.TestcontainersConfiguration;
import com.alaa.banking.accountservice.common.error.ApiErrorResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerCreatesUserAndReturnsIt() {
        RegisterRequest request = new RegisterRequest(uniqueEmail(), "s3cret-password");

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(request.email());
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() {
        String email = uniqueEmail();
        RegisterRequest request = new RegisterRequest(email, "s3cret-password");
        restTemplate.postForEntity("/api/v1/auth/register", request, UserResponse.class);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void registerWithInvalidPayloadReturnsBadRequest() {
        RegisterRequest request = new RegisterRequest("not-an-email", "short");

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loginWithCorrectCredentialsReturnsAccessToken() {
        String email = uniqueEmail();
        String password = "s3cret-password";
        restTemplate.postForEntity("/api/v1/auth/register", new RegisterRequest(email, password), UserResponse.class);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, password), LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().expiresInSeconds()).isPositive();
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() {
        String email = uniqueEmail();
        restTemplate.postForEntity("/api/v1/auth/register", new RegisterRequest(email, "s3cret-password"), UserResponse.class);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "wrong-password"), ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithUnknownEmailReturnsUnauthorized() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(uniqueEmail(), "whatever-password"), ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointRejectsRequestWithoutToken() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "/api/v1/auth/me", ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointAcceptsRequestWithValidToken() {
        String email = uniqueEmail();
        String password = "s3cret-password";
        restTemplate.postForEntity("/api/v1/auth/register", new RegisterRequest(email, password), UserResponse.class);
        LoginResponse login = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, password), LoginResponse.class).getBody();
        assertThat(login).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.accessToken());
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/v1/auth/me", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(email);
    }

    @Test
    void protectedEndpointRejectsInvalidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-real-token");
        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                "/api/v1/auth/me", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void jwksEndpointIsPubliclyAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/.well-known/jwks.json", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"keys\"");
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
