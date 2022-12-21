import {useTheme} from "@mui/material";
import {tokens} from "../../theme";
import {useNavigate} from "react-router-dom";
import {useDispatch} from "react-redux";
import {Dispatch} from "../../redux/store";


const Logout = () => {
    const theme = useTheme();
    const colors = tokens(theme.palette.mode);
    const navigateTo = useNavigate();
    const dispatch = useDispatch<Dispatch>()

    console.log("The logout component was just  rendered!");

    window.localStorage.removeItem('jwtToken');
    window.localStorage.removeItem('rolesFromJwt');
    window.localStorage.removeItem('jwtExpiresAt');

    return (
        <div>
            You're now logged out!
        </div>
    );
}

export default Logout;