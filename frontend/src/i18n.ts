export const i18n = {
  general: {
    appName: "IMDb Clone",
    profile: "Profile",
    messages: "Messages",
  },
  regex: {
    username: {
      matches: (password: string) =>  password.match(i18n.regex.username.pattern),
      pattern: "^(?=.{2,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$",
      rules: "must be between 2-20 characters, only . and _ ", // TODO
    },
    email: {
      pattern:
        /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
      rules: "must be valid email address", // TODO
    },
    password: {
      pattern: "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])(?=.{8,30})",
      rules: "password must be between 8 and 30 characters and more", // TODO
    },
  },
  menuOptions: {
    logout: "Logout",
  },
  accountSettings: {
    username: "Username",
    email: "Email Address",
    firstName: "First Name",
    lastName: "Last Name",
    birthday: "Birthday",
    phone: "Phone Number",
    bio: "Biography",
    submit: "Update",
    successfulUpdate: "Account Profile was updated successfully",
    loadingError: (action: string) => `Error while attempting to ${action}`,
  },
  home: {},
  registration: {
    registrationSuccessful: "You have been registered successfully",
    loadingError: "Error while attempting to register",
  },
  login: {
    badCredentials: "Bad Credentials",
    loadingError: "Error while attempting to login",
  },
  logout: {},

  watchlist: {},
  ratings: {},
  editMovie: {},
  messages: {},
  movieSearch: {},
};
