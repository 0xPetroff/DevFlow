package com.devflow.security;

import com.devflow.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class DevFlowUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DevFlowUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Accepts either an email address or a username so the login form can take both. */
    @Override
    public UserDetails loadUserByUsername(String identifier) {
        String normalised = identifier == null ? "" : identifier.trim();
        return userRepository.findByEmail(normalised.toLowerCase(Locale.ROOT))
                .or(() -> userRepository.findByUsername(normalised))
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("No account matches " + normalised));
    }
}
