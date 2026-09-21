package com.smhrd.hometraining.security;

import com.smhrd.hometraining.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        return userRepository.findByLoginId(loginId)
                .map(CustomUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다: " + loginId));
    }

    public UserDetails loadUserById(Long userId) {
        return userRepository.findById(userId)
                .map(CustomUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다: " + userId));
    }
}
