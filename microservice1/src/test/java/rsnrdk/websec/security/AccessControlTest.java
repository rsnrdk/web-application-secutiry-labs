package rsnrdk.websec.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 7: access control tests for URL rules (lab 5) and method annotations (lab 6).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("1. Anonymous can open the public route")
    void anonymous_publicRoute_ok() throws Exception {
        mockMvc.perform(get("/api/v1/access/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("Public access granted"));
    }

    @Test
    @DisplayName("2. Anonymous is rejected on the user route with 401")
    void anonymous_userRoute_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/access/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("3. USER can open the user route")
    void user_userRoute_ok() throws Exception {
        mockMvc.perform(get("/api/v1/access/user"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("4. USER is forbidden on the admin route")
    void user_adminRoute_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/access/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("5. ADMIN can open the admin route")
    void admin_adminRoute_ok() throws Exception {
        mockMvc.perform(get("/api/v1/access/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("6. ADMIN is forbidden on the superadmin route")
    void admin_superAdminRoute_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/access/superadmin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("7. USER cannot create items")
    void user_createItem_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\",\"description\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("8. ADMIN cannot delete items (SUPERADMIN only)")
    void admin_deleteItem_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/items/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("9. USER is forbidden on the @Secured admin route")
    void user_securedAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/annotated/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("10. ADMIN is forbidden on the @RolesAllowed superadmin route")
    void admin_rolesAllowedSuperAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/annotated/superadmin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("11. USER can read own details but not someone else's")
    void user_ownDetailsOnly() throws Exception {
        mockMvc.perform(get("/api/v1/annotated/users/user"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/annotated/users/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Seeded SUPERADMIN with real JWT opens the superadmin route")
    void superAdmin_realJwt_ok() throws Exception {
        String token = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"superadmin\",\"password\":\"superadmin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/annotated/superadmin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Superadmin access granted via @RolesAllowed"));
    }
}
