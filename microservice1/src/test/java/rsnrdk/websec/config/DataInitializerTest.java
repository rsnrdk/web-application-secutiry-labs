package rsnrdk.websec.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import rsnrdk.websec.entity.Role;
import rsnrdk.websec.entity.User;
import rsnrdk.websec.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_emptyDatabase_createsThreeUsersWithRolesAndEncodedPasswords() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "enc:" + inv.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(captor.capture());
        List<User> saved = captor.getAllValues();
        assertEquals("user", saved.get(0).getUsername());
        assertEquals(Role.USER, saved.get(0).getRole());
        assertEquals("enc:user123", saved.get(0).getPassword());
        assertEquals("admin", saved.get(1).getUsername());
        assertEquals(Role.ADMIN, saved.get(1).getRole());
        assertEquals("superadmin", saved.get(2).getUsername());
        assertEquals(Role.SUPERADMIN, saved.get(2).getRole());
    }

    @Test
    void run_usersAlreadyExist_doesNotCreateDuplicates() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void run_onlyAdminExists_createsOnlyMissingUsers() {
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.existsByUsername("superadmin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        dataInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        assertEquals(List.of("user", "superadmin"),
                captor.getAllValues().stream().map(User::getUsername).toList());
        verify(passwordEncoder, never()).encode("admin123");
    }
}