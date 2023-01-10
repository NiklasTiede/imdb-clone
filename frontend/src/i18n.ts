export const i18n = {
  general: {
    appName: "IMDb Clone",
    profile: "Profile",
    messages: "Messages",
  },
  regex: {
    username: "",
    email: "",
    password: "",
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
  logout: {},
  login: {
    badCredentials: "Bad Credentials",
    loadingError: "Error while attempting to login",
  },
  registration: {
    registrationSuccessful: "You have been registered successfully",
    loadingError: "Error while attempting to register",
  },
  watchlist: {},
  ratings: {},
  editMovie: {},
  messages: {},
  movieSearch: {},
};
