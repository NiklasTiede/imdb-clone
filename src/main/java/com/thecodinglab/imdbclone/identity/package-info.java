@ApplicationModule(
    id = "identity",
    displayName = "Identity",
    allowedDependencies = {
      "account::identity",
      "shared::api",
      "shared::error",
      "shared::security",
      "shared::validation"
    })
package com.thecodinglab.imdbclone.identity;

import org.springframework.modulith.ApplicationModule;
