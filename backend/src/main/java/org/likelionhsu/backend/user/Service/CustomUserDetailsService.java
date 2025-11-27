package org.likelionhsu.backend.user.Service;

import lombok.RequiredArgsConstructor;
import org.likelionhsu.backend.user.Enitity.User;
import org.likelionhsu.backend.user.Repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (user.getStatus() == User.UserStatus.DELETED) {
            throw new UsernameNotFoundException("User is deleted");
        }

        return new UserDetailsImpl(user);
    }
}

