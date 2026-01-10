package com.nexusos.api.workspace.controller;

import com.nexusos.api.identity.domain.User;
import com.nexusos.api.identity.repository.UserRepository;
import com.nexusos.api.identity.security.JwtService;
import com.nexusos.api.workspace.domain.Membership;
import com.nexusos.api.workspace.domain.Organization;
import com.nexusos.api.workspace.domain.Role;
import com.nexusos.api.workspace.domain.Workspace;
import com.nexusos.api.workspace.repository.MembershipRepository;
import com.nexusos.api.workspace.repository.OrganizationRepository;
import com.nexusos.api.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.nexusos.api.search.repository.DocumentSearchRepository;

import jakarta.servlet.http.Cookie;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProjectControllerSecurityTest {

    @MockBean
    private DocumentSearchRepository documentSearchRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Workspace workspace;
    private String memberToken;
    private String nonMemberToken;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        workspaceRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        User memberUser = userRepository.save(new User("member@example.com", passwordEncoder.encode("Pass123!"), "Member"));
        User nonMemberUser = userRepository.save(new User("nonmember@example.com", passwordEncoder.encode("Pass123!"), "NonMember"));

        Organization org = organizationRepository.save(new Organization("Test Org", "test-org"));
        workspace = workspaceRepository.save(new Workspace(org, "Test Workspace", "test-workspace"));

        membershipRepository.save(new Membership(memberUser, workspace, Role.MEMBER));

        // Build Spring Security UserDetails for JWT generation
        org.springframework.security.core.userdetails.UserDetails memberDetails =
                new org.springframework.security.core.userdetails.User(
                        memberUser.getEmail(), memberUser.getPasswordHash(), Collections.emptyList());
        org.springframework.security.core.userdetails.UserDetails nonMemberDetails =
                new org.springframework.security.core.userdetails.User(
                        nonMemberUser.getEmail(), nonMemberUser.getPasswordHash(), Collections.emptyList());

        memberToken = jwtService.generateToken(memberDetails);
        nonMemberToken = jwtService.generateToken(nonMemberDetails);
    }

    @Test
    void testGetProjectsAsMember_Success() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/" + workspace.getId() + "/projects")
                .cookie(new Cookie("nexusos_access_token", memberToken)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetProjectsAsNonMember_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/" + workspace.getId() + "/projects")
                .cookie(new Cookie("nexusos_access_token", nonMemberToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetProjectsUnauthenticated_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/" + workspace.getId() + "/projects"))
                .andExpect(status().isForbidden());
    }
}
