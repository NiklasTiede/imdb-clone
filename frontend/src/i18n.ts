export const i18n = {
  general: {
    appName: "IMDb Clone",
    profile: "Profile",
    messages: "Messages",
  },
  regex: {
    username: {
      pattern: "^(?=.{2,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$",
      rules:
        "Username must be between 2 - 20 characters and can contain digits, dots or hyphens",
    },
    email: {
      pattern:
        /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
      rules: "Email must be a valid address",
    },
    password: {
      pattern: "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,30}",
      rules:
        "Password must be between 8 and 30 characters and contain a digit, a lower-, upper- and special character",
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
    register: "Register",
    registrationSuccessful: "You have been registered successfully",
    loadingError: "Error while attempting to register",
  },
  login: {
    login: "Login",
    registration: "Registration",
    badCredentials: "Bad Credentials",
    loadingError: "Error while attempting to login",
  },
  logout: {
    message: "You are now logged out.",
  },
  watchlist: {},
  ratings: {},
  messages: {},
  editMovie: {},
  movieSearch: {},
  notFound: {
    message: "This page does not exist.",
  },
  accessDenied: {
    warning: "Access Denied",
    message: (role: string) =>
      `Permission '${role}' is required to access this resource.`,
  },
};
