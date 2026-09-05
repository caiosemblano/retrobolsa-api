package com.retrobolsa.api.controller;

import com.retrobolsa.api.security.JwtUtil;
import com.retrobolsa.api.user.User;
import com.retrobolsa.api.user.UserRepository;
import com.retrobolsa.api.user.dto.LoginRequest;
import com.retrobolsa.api.user.dto.RegisterRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do fluxo de autenticação (registro, login e proteção
 * de rotas via JWT), rodando contra um Postgres real via Testcontainers.
 * <p>
 * Como não ativamos o profile "test", o Flyway roda de verdade aqui,
 * aplicando as migrations reais contra o banco do container —
 * diferente dos testes que usam H2, isso valida o schema de produção.
 */
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String EMAIL_PADRAO = "rafael.teste@retrobolsa.com";
    private static final String SENHA_PADRAO = "senhaForte123";

    @BeforeEach
    void limparBanco() {
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------------
    // POST /api/auth/register
    // ---------------------------------------------------------------

    @Test
    void deveRegistrarUsuarioComDadosValidos() throws Exception {
        RegisterRequest request = novoRegisterRequest(EMAIL_PADRAO, "rafael", SENHA_PADRAO, SENHA_PADRAO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> salvo = userRepository.findByEmail(EMAIL_PADRAO);
        assertThat(salvo).isPresent();
        assertThat(salvo.get().getUsername()).isEqualTo("rafael");
        // senha nunca deve ser persistida em texto plano
        assertThat(salvo.get().getPasswordHash()).isNotEqualTo(SENHA_PADRAO);
        assertThat(passwordEncoder.matches(SENHA_PADRAO, salvo.get().getPasswordHash())).isTrue();
        assertThat(salvo.get().getRole()).isEqualTo("PLAYER");
    }

    @Test
    void deveRegistrarComoAdminQuandoEmailForDoDominioAdmin() throws Exception {
        RegisterRequest request = novoRegisterRequest("retroteste@admin.com", "adminteste", SENHA_PADRAO, SENHA_PADRAO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> salvo = userRepository.findByEmail("retroteste@admin.com");
        assertThat(salvo).isPresent();
        assertThat(salvo.get().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void deveRejeitarRegistroComEmailDuplicado() throws Exception {
        criarUsuarioNoBanco(EMAIL_PADRAO, "primeiroUsuario", SENHA_PADRAO);

        RegisterRequest request = novoRegisterRequest(EMAIL_PADRAO, "segundoUsuario", SENHA_PADRAO, SENHA_PADRAO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("email")));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void deveRejeitarRegistroComUsernameDuplicado() throws Exception {
        criarUsuarioNoBanco("outro@retrobolsa.com", "rafael", SENHA_PADRAO);

        RegisterRequest request = novoRegisterRequest(EMAIL_PADRAO, "rafael", SENHA_PADRAO, SENHA_PADRAO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("usuário")));
    }

    @Test
    void deveRejeitarRegistroComSenhasDiferentes() throws Exception {
        RegisterRequest request = novoRegisterRequest(EMAIL_PADRAO, "rafael", SENHA_PADRAO, "outraSenha123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void deveRejeitarRegistroComEmailEmFormatoInvalido() throws Exception {
        RegisterRequest request = novoRegisterRequest("nao-e-um-email", "rafael", SENHA_PADRAO, SENHA_PADRAO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[*].campo", hasItem("email")));
    }

    @Test
    void deveRejeitarRegistroComSenhaCurta() throws Exception {
        RegisterRequest request = novoRegisterRequest(EMAIL_PADRAO, "rafael", "123", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[*].campo", hasItem("senha")));
    }

    @Test
    void deveRejeitarRegistroComCamposObrigatoriosEmBranco() throws Exception {
        RegisterRequest request = novoRegisterRequest("", "", "", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", not(empty())));
    }

    // ---------------------------------------------------------------
    // POST /api/auth/login
    // ---------------------------------------------------------------

    @Test
    void deveLogarComCredenciaisValidasERetornarTokenJwtValido() throws Exception {
        criarUsuarioNoBanco(EMAIL_PADRAO, "rafael", SENHA_PADRAO);

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL_PADRAO);
        request.setSenha(SENHA_PADRAO);

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseBody).get("token").asText();
        assertThat(jwtUtil.validateJwtToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(EMAIL_PADRAO);
    }

    @Test
    void deveRejeitarLoginComSenhaIncorreta() throws Exception {
        criarUsuarioNoBanco(EMAIL_PADRAO, "rafael", SENHA_PADRAO);

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL_PADRAO);
        request.setSenha("senhaErrada123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Credenciais")));
    }

    @Test
    void deveRejeitarLoginComEmailNaoCadastrado() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("naoexiste@retrobolsa.com");
        request.setSenha(SENHA_PADRAO);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("Credenciais")));
    }

    @Test
    void deveRejeitarLoginComEmailEmFormatoInvalido() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nao-e-um-email");
        request.setSenha(SENHA_PADRAO);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveBloquearLoginApos5TentativasEm1Minuto() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("bruteforce@retrobolsa.com");
        request.setSenha("senhaErrada123");
        String requestJson = objectMapper.writeValueAsString(request);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.erro", containsString("Muitas tentativas")));
    }

    // ---------------------------------------------------------------
    // Proteção de rotas (filtro JWT)
    // ---------------------------------------------------------------

    @Test
    void deveNegarAcessoAEndpointProtegidoSemToken() throws Exception {
        mockMvc.perform(get("/api/portfolios/my-last-result"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarAcessoAEndpointProtegidoComTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/portfolios/my-last-result")
                        .header("Authorization", "Bearer token-invalido-e-mal-formado"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private RegisterRequest novoRegisterRequest(String email, String username, String senha, String confirmarSenha) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setUsername(username);
        request.setSenha(senha);
        request.setConfirmarSenha(confirmarSenha);
        return request;
    }

    private User criarUsuarioNoBanco(String email, String username, String senhaPura) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(senhaPura))
                .build();
        return userRepository.save(user);
    }
}
