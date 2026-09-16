@ApplicationModule(
    id = "account",
    displayName = "Accounts",
    allowedDependencies = {
      "engagement::profile",
      "engagement::lifecycle",
      "shared::api",
      "shared::error",
      "shared::persistence",
      "shared::security",
      "shared::validation"
    })
package app.popcornsociety.account;

import org.springframework.modulith.ApplicationModule;
