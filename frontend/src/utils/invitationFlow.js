const PENDING_INVITATION_KEY = 'pendingCommunityInvitation';

const toNumberOrNull = (value) => {
  if (value === null || value === undefined || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

export const savePendingInvitation = ({ code, communityId = null }) => {
  if (!code || typeof code !== 'string') return;

  const payload = {
    code: code.trim(),
    communityId: toNumberOrNull(communityId),
    createdAt: Date.now(),
  };

  localStorage.setItem(PENDING_INVITATION_KEY, JSON.stringify(payload));
};

export const getPendingInvitation = () => {
  try {
    const raw = localStorage.getItem(PENDING_INVITATION_KEY);
    if (!raw) return null;

    const parsed = JSON.parse(raw);
    if (!parsed?.code || typeof parsed.code !== 'string') return null;

    return {
      code: parsed.code,
      communityId: toNumberOrNull(parsed.communityId),
      createdAt: parsed.createdAt || null,
    };
  } catch {
    return null;
  }
};

export const clearPendingInvitation = () => {
  localStorage.removeItem(PENDING_INVITATION_KEY);
};

export const buildPendingInvitationPath = (pendingInvitation) => {
  if (!pendingInvitation?.code) return null;

  const communityId = toNumberOrNull(pendingInvitation.communityId);
  const query = communityId ? `?communityId=${communityId}` : '';
  return `/invitacion/${encodeURIComponent(pendingInvitation.code)}/aceptar${query}`;
};

export const getPendingInvitationPath = () => {
  const pendingInvitation = getPendingInvitation();
  return buildPendingInvitationPath(pendingInvitation);
};
