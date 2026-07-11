import DeleteOutlineIcon from "@mui/icons-material/DeleteOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import IconButton from "@mui/material/IconButton";
import ListItemIcon from "@mui/material/ListItemIcon";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useState } from "react";
import { ProfileAvatar } from "../../../../shared/media";
import type { CommentAuthor, MovieComment } from "../model/comment";
import {
  formatCommentTime,
  formatCommentTimeTitle,
  wasCommentEdited,
} from "../utils/commentTime";

const MAX_COMMENT_LENGTH = 1000;

type CommentItemProps = {
  author?: CommentAuthor;
  canManage: boolean;
  comment: MovieComment;
  isDeleting: boolean;
  isUpdating: boolean;
  onDelete: (commentId: number) => Promise<void>;
  onUpdate: (commentId: number, message: string) => Promise<void>;
};

const CommentItem = ({
  author,
  canManage,
  comment,
  isDeleting,
  isUpdating,
  onDelete,
  onUpdate,
}: CommentItemProps) => {
  const [anchorElement, setAnchorElement] = useState<HTMLElement | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [draft, setDraft] = useState(comment.message ?? "");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const commentId = comment.id;
  const authorName =
    author?.displayName?.trim() ||
    author?.username?.trim() ||
    (comment.accountId ? `Member ${comment.accountId}` : "Former member");
  const username = author?.username?.trim();

  const saveEdit = async () => {
    const normalizedDraft = draft.trim();
    if (!commentId || !normalizedDraft) {
      setErrorMessage("A comment cannot be empty.");
      return;
    }

    try {
      setErrorMessage(null);
      await onUpdate(commentId, normalizedDraft);
      setIsEditing(false);
    } catch {
      setErrorMessage("Could not update this comment. Please try again.");
    }
  };

  const confirmDelete = async () => {
    if (!commentId) {
      return;
    }
    try {
      setErrorMessage(null);
      await onDelete(commentId);
      setDeleteDialogOpen(false);
    } catch {
      setErrorMessage("Could not delete this comment. Please try again.");
    }
  };

  return (
    <Box
      component="article"
      data-testid={commentId ? `comment-${commentId}` : undefined}
      id={commentId ? `comment-${commentId}` : undefined}
      sx={{
        display: "grid",
        gap: 1.5,
        gridTemplateColumns: "40px minmax(0, 1fr)",
        py: { xs: 2, sm: 2.25 },
      }}
    >
      <ProfileAvatar
        imageUrlToken={author?.imageUrlToken}
        sx={{ height: 40, width: 40 }}
      />
      <Box sx={{ minWidth: 0 }}>
        <Stack
          direction="row"
          spacing={0.75}
          sx={{ alignItems: "center", minHeight: 32 }}
        >
          <Typography sx={{ fontSize: 14, fontWeight: 700 }}>
            {authorName}
          </Typography>
          {username && authorName !== username && (
            <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
              @{username}
            </Typography>
          )}
          <Typography aria-hidden sx={{ color: "text.secondary" }}>
            ·
          </Typography>
          <Box
            component="time"
            dateTime={comment.createdAtInUtc}
            title={formatCommentTimeTitle(comment.createdAtInUtc)}
            sx={{ color: "text.secondary", fontSize: 12 }}
          >
            {formatCommentTime(comment.createdAtInUtc)}
          </Box>
          {wasCommentEdited(
            comment.createdAtInUtc,
            comment.modifiedAtInUtc,
          ) && (
            <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
              (edited)
            </Typography>
          )}
          {canManage && (
            <IconButton
              aria-label={`Manage comment by ${authorName}`}
              onClick={(event) => setAnchorElement(event.currentTarget)}
              size="small"
              sx={{ ml: "auto" }}
            >
              <MoreVertIcon fontSize="small" />
            </IconButton>
          )}
        </Stack>

        {isEditing ? (
          <Box sx={{ mt: 1 }}>
            <TextField
              disabled={isUpdating}
              fullWidth
              label="Edit comment"
              maxRows={8}
              minRows={3}
              multiline
              onChange={(event) => setDraft(event.target.value)}
              slotProps={{ htmlInput: { maxLength: MAX_COMMENT_LENGTH } }}
              value={draft}
            />
            <Stack
              direction="row"
              spacing={1}
              sx={{ alignItems: "center", justifyContent: "flex-end", mt: 1 }}
            >
              <Typography
                sx={{ color: "text.secondary", fontSize: 12, mr: "auto" }}
              >
                {draft.length} / {MAX_COMMENT_LENGTH}
              </Typography>
              <Button
                disabled={isUpdating}
                onClick={() => {
                  setDraft(comment.message ?? "");
                  setErrorMessage(null);
                  setIsEditing(false);
                }}
              >
                Cancel
              </Button>
              <Button
                disabled={isUpdating || !draft.trim()}
                onClick={() => void saveEdit()}
                variant="contained"
              >
                {isUpdating ? "Saving..." : "Save"}
              </Button>
            </Stack>
          </Box>
        ) : (
          <Typography
            sx={{
              color: "rgba(255,255,255,0.88)",
              fontSize: { xs: 14, sm: 15 },
              lineHeight: 1.7,
              overflowWrap: "anywhere",
              whiteSpace: "pre-wrap",
            }}
          >
            {comment.message}
          </Typography>
        )}

        {errorMessage && !deleteDialogOpen && (
          <Alert severity="error" sx={{ mt: 1.25 }}>
            {errorMessage}
          </Alert>
        )}
      </Box>

      <Menu
        anchorEl={anchorElement}
        onClose={() => setAnchorElement(null)}
        open={Boolean(anchorElement)}
      >
        <MenuItem
          onClick={() => {
            setAnchorElement(null);
            setDraft(comment.message ?? "");
            setErrorMessage(null);
            setIsEditing(true);
          }}
        >
          <ListItemIcon>
            <EditOutlinedIcon fontSize="small" />
          </ListItemIcon>
          Edit
        </MenuItem>
        <MenuItem
          onClick={() => {
            setAnchorElement(null);
            setErrorMessage(null);
            setDeleteDialogOpen(true);
          }}
        >
          <ListItemIcon>
            <DeleteOutlineIcon fontSize="small" />
          </ListItemIcon>
          Delete
        </MenuItem>
      </Menu>

      <Dialog
        aria-labelledby={`delete-comment-${commentId ?? "unknown"}-title`}
        onClose={() => !isDeleting && setDeleteDialogOpen(false)}
        open={deleteDialogOpen}
      >
        <DialogTitle id={`delete-comment-${commentId ?? "unknown"}-title`}>
          Delete comment?
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            This permanently removes your comment from the movie discussion.
          </DialogContentText>
          {errorMessage && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {errorMessage}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            disabled={isDeleting}
            onClick={() => setDeleteDialogOpen(false)}
          >
            Cancel
          </Button>
          <Button
            color="error"
            disabled={isDeleting}
            onClick={() => void confirmDelete()}
            variant="contained"
          >
            {isDeleting ? "Deleting..." : "Delete"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CommentItem;
