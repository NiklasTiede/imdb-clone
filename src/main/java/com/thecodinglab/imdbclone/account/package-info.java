@ApplicationModule(
    id = "account",
    displayName = "Accounts",
    allowedDependencies = {
      "engagement::profile",
      "shared::api",
      "shared::error",
      "shared::persistence",
      "shared::security",
      "shared::validation"
    })
package com.thecodinglab.imdbclone.account;

import org.springframework.modulith.ApplicationModule;
