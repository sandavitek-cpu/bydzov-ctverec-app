package cz.previt.bydzovctverec.config;

import cz.previt.bydzovctverec.domain.User;
import cz.previt.bydzovctverec.domain.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository
        .findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    var authorities = user.getAppRoles().isEmpty()
        ? List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        : user.getAppRoles().stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
            .toList();
    return new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getPassword(), authorities);
  }
}
