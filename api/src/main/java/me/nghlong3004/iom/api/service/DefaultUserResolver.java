package me.nghlong3004.iom.api.service;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.iom.api.application.port.out.UserResolver;
import me.nghlong3004.iom.api.domain.message.IncomingMessage;
import me.nghlong3004.iom.api.domain.user.AppUser;
import me.nghlong3004.iom.api.domain.user.ExternalAccount;
import me.nghlong3004.iom.api.repository.AppUserRepository;
import me.nghlong3004.iom.api.repository.ExternalAccountRepository;
import me.nghlong3004.iom.api.service.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link UserResolver} implementation. Looks up the user by their external platform
 * identity, auto-provisioning a new {@link AppUser} on first encounter.
 *
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserResolver implements UserResolver {

  private final ExternalAccountRepository externalAccountRepository;
  private final AppUserRepository appUserRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public AppUser resolve(IncomingMessage message) {
    Objects.requireNonNull(message, "message must not be null");

    var platform = message.channel();
    var externalUserId = message.externalUserId();

    return externalAccountRepository
        .findByPlatformAndExternalUserId(platform, externalUserId)
        .map(ExternalAccount::getUser)
        .orElseGet(
            () -> {
              var newUser = userMapper.toNewAppUser();
              var savedUser = appUserRepository.save(newUser);

              var externalAccount =
                  userMapper.toExternalAccount(savedUser, platform, externalUserId);
              externalAccountRepository.save(externalAccount);

              log.info(
                  "Auto-provisioned new user: id={}, platform={}, externalUserId={}",
                  savedUser.getId(),
                  platform,
                  externalUserId);

              return savedUser;
            });
  }
}
