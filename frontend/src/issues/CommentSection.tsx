import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import { useAuth } from '@/auth/useAuth';
import { Alert } from '@/components/ui/Alert';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Textarea } from '@/components/ui/Input';
import { Spinner } from '@/components/ui/Spinner';
import { errorMessage } from '@/lib/apiError';
import { formatRelative } from '@/lib/format';
import { queryKeys } from '@/lib/queryKeys';
import { issuesApi } from '@/issues/issuesApi';
import { useProject } from '@/projects/ProjectContext';
import { useToast } from '@/toast/useToast';
import type { CommentResponse, Uuid } from '@/types/api';

export function CommentSection({ issueId }: { issueId: Uuid }) {
  const { user } = useAuth();
  const { writer, admin } = useProject();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [body, setBody] = useState('');
  const [editing, setEditing] = useState<CommentResponse | null>(null);
  const [editBody, setEditBody] = useState('');
  const [deleting, setDeleting] = useState<CommentResponse | null>(null);

  const commentsKey = queryKeys.issueComments(issueId);
  const { data, isPending, error } = useQuery({
    queryKey: commentsKey,
    queryFn: () => issuesApi.comments(issueId),
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: commentsKey });

  const create = useMutation({
    mutationFn: () => issuesApi.createComment(issueId, { body }),
    onSuccess: async () => {
      setBody('');
      await refresh();
    },
    onError: (createError) => {
      toast.failure(createError, 'Could not post that comment');
    },
  });

  const update = useMutation({
    mutationFn: (comment: CommentResponse) =>
      issuesApi.updateComment(issueId, comment.id, { body: editBody }),
    onSuccess: async () => {
      setEditing(null);
      await refresh();
    },
    onError: (updateError) => {
      toast.failure(updateError, 'Could not save that edit');
    },
  });

  const remove = useMutation({
    mutationFn: (comment: CommentResponse) => issuesApi.removeComment(issueId, comment.id),
    onSuccess: async () => {
      setDeleting(null);
      toast.success('Comment deleted');
      await refresh();
    },
    onError: (removeError) => {
      toast.failure(removeError, 'Could not delete that comment');
    },
  });

  const comments = data?.content ?? [];

  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-ink text-sm font-semibold">
        Comments{comments.length > 0 && ` (${data?.totalElements ?? comments.length})`}
      </h2>

      {error ? (
        <Alert variant="danger">{errorMessage(error)}</Alert>
      ) : isPending ? (
        <div className="text-muted flex justify-center py-6">
          <Spinner />
        </div>
      ) : comments.length === 0 ? (
        <p className="text-muted text-sm">No comments yet.</p>
      ) : (
        <ul className="flex flex-col gap-4">
          {comments.map((comment) => {
            const mine = comment.author.id === user?.id;
            const isEditing = editing?.id === comment.id;
            return (
              <li key={comment.id} className="flex gap-3">
                <Avatar name={comment.author.fullName} color={comment.author.avatarColor} />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-baseline gap-x-2">
                    <span className="text-ink text-sm font-medium">{comment.author.fullName}</span>
                    <span className="text-faint text-xs">{formatRelative(comment.createdAt)}</span>
                    {comment.edited && <span className="text-faint text-xs">edited</span>}
                  </div>

                  {isEditing ? (
                    <div className="mt-2 flex flex-col gap-2">
                      <Textarea
                        value={editBody}
                        rows={3}
                        aria-label="Edit comment"
                        onChange={(event) => {
                          setEditBody(event.target.value);
                        }}
                      />
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          loading={update.isPending}
                          disabled={editBody.trim() === ''}
                          onClick={() => {
                            update.mutate(comment);
                          }}
                        >
                          Save
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            setEditing(null);
                          }}
                        >
                          Cancel
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <p className="text-muted mt-1 text-sm whitespace-pre-wrap">{comment.body}</p>
                  )}

                  {!isEditing && (mine || admin) && (
                    <div className="mt-1 flex gap-3">
                      {/* Editing is author-only on the API; an administrator may only delete. */}
                      {mine && (
                        <button
                          type="button"
                          aria-label="Edit your comment"
                          className="text-faint hover:text-ink cursor-pointer text-xs"
                          onClick={() => {
                            setEditing(comment);
                            setEditBody(comment.body);
                          }}
                        >
                          Edit
                        </button>
                      )}
                      <button
                        type="button"
                        aria-label={`Delete comment from ${comment.author.fullName}`}
                        className="text-faint hover:text-danger cursor-pointer text-xs"
                        onClick={() => {
                          setDeleting(comment);
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {writer && (
        <div className="flex flex-col gap-2">
          <Textarea
            value={body}
            rows={3}
            aria-label="Add a comment"
            placeholder="Leave a comment"
            onChange={(event) => {
              setBody(event.target.value);
            }}
          />
          <div>
            <Button
              size="sm"
              loading={create.isPending}
              disabled={body.trim() === ''}
              onClick={() => {
                create.mutate();
              }}
            >
              Comment
            </Button>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="Delete comment"
        confirmLabel="Delete"
        loading={remove.isPending}
        onCancel={() => {
          setDeleting(null);
        }}
        onConfirm={() => {
          if (deleting) {
            remove.mutate(deleting);
          }
        }}
      >
        This cannot be undone.
      </ConfirmDialog>
    </section>
  );
}
