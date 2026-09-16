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
package app.popcornsociety.identity;

import org.springframework.modulith.ApplicationModule;
