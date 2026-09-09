package com.thecodinglab.imdbclone.account.api;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import org.springframework.modulith.NamedInterface;

@NamedInterface("media")
public interface AccountImageService {

  AccountImageToken getProfileImageToken(UserPrincipal currentUser);

  AccountImageToken updateProfileImageToken(Long accountId, String imageUrlToken);

  void clearProfileImageToken(Long accountId);

  boolean isProfileImageTokenReferenced(String token);
}
