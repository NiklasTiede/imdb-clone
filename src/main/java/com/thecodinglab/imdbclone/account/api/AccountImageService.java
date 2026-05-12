package com.thecodinglab.imdbclone.account.api;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;

public interface AccountImageService {

  AccountImageToken getProfileImageToken(UserPrincipal currentUser);

  AccountImageToken updateProfileImageToken(Long accountId, String imageUrlToken);
}
