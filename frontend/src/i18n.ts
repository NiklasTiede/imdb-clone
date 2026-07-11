export const i18n = {
  general: {
    appName: "IMDb Clone",
    profile: "Profile",
    messages: "Messages",
  },
  regex: {
    username: {
      pattern: "^(?=.{2,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]*[a-zA-Z0-9]$",
      rules:
        "Use 2-20 letters, numbers, dots, or underscores; dots and underscores cannot be leading, trailing, or consecutive",
    },
    email: {
      pattern:
        /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
      rules: "Email must be a valid address",
    },
    password: {
      pattern: "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[#?!@$%^&*-]).{8,30}$",
      rules:
        "Use 8-30 characters with an uppercase letter, lowercase letter, number, and special character",
    },
  },
  menuOptions: {
    logout: "Logout",
  },
  accountSettings: {
    accountSaved: "Account saved",
    username: "Username",
    email: "Email Address",
    firstName: "First Name",
    lastName: "Last Name",
    birthday: "Birthday",
    phone: "Phone Number",
    bio: "Biography",
    profilePhotoRemoved: "Profile photo removed",
    profileSaved: "Profile saved",
    submit: "Update",
    successfulUpdate: "Account Profile was updated successfully",
    loadingError: (action: string) => `Error while attempting to ${action}`,
  },
  home: {
    heading: "Home Page",
  },
  registration: {
    register: "Register",
    loadingError: "Error while attempting to register",
  },
  login: {
    login: "Login",
    registration: "Registration",
  },
  logout: {
    message: "You are now logged out.",
  },
  watchlist: {
    empty: "Your watchlist is empty.",
    heading: "Your watchlist",
    loadingError: "Error while attempting to load your watchlist",
  },
  ratings: {
    empty: "You have not rated any movies yet.",
    heading: "Your Ratings",
    loadingError: "Error while attempting to load your ratings",
  },
  messages: {
    heading: "Your Messages",
  },
  editMovie: {
    heading: "Edit/Create Movie",
  },
  movieSearch: {},
  movieDetails: {
    loadingError: (movieId: string | null) =>
      `Error while attempting to get movie details about title with id ${movieId}`,
  },
  notFound: {
    message: "This page does not exist.",
  },
  accessDenied: {
    warning: "Access Denied",
    message: (role: string) =>
      `Permission '${role}' is required to access this resource.`,
  },
};
