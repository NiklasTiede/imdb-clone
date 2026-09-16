package app.popcornsociety.account.api;

import app.popcornsociety.shared.api.MessageResponse;
import app.popcornsociety.shared.security.UserPrincipal;

public interface RoleService {

  MessageResponse giveAdminRole(String username, UserPrincipal currentAccount);

  MessageResponse removeAdminRole(String username, UserPrincipal currentAccount);
}
