package me.nghlong3004.iom.api.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.exception.ErrorCode;
import me.nghlong3004.iom.api.exception.ResourceException;
import me.nghlong3004.iom.api.repository.AppUserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

  private final AppUserRepository appUserRepository;

  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
    AppUser appUser =
        appUserRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceException(ErrorCode.EMAIL_NOT_FOUND));

    return new User(
        appUser.getEmail(),
        appUser.getPasswordHash() != null ? appUser.getPasswordHash() : "",
        appUser.getIsActive(),
        true,
        true,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())));
  }
}
