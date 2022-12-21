import {useDispatch} from "react-redux";
import {Dispatch} from "../../redux/store";
import {useTheme} from "@mui/material";
import {tokens} from "../../theme";
import {useNavigate} from "react-router-dom";


const AccountSettings = () => {
    const theme = useTheme();
    const colors = tokens(theme.palette.mode);
    const navigateTo = useNavigate();
    const dispatch = useDispatch<Dispatch>()

    dispatch.account.getCurrentAccount();

    return (
        <div>
            Account Settings
        </div>
    );
}

export default AccountSettings;