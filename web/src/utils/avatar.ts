export interface AvatarIdentity {
  nickname?: string | null
  username?: string | null
}

export interface AvatarPresentation {
  displayName: string
  initial: string
  backgroundColor: string
}

const AVATAR_COLORS = ['#4f6f8f', '#5f7f6b', '#8a6d5d', '#6f6792', '#397b78', '#8a6670']

function stableColorIndex(value: string): number {
  let hash = 0
  for (const character of value) {
    hash = (hash * 31 + (character.codePointAt(0) ?? 0)) >>> 0
  }
  return hash % AVATAR_COLORS.length
}

export function getAvatarPresentation(identity: AvatarIdentity): AvatarPresentation {
  const nickname = identity.nickname?.trim() ?? ''
  const username = identity.username?.trim() ?? ''
  const sourceName = nickname || username
  const displayName = sourceName || '用户'
  const initial = sourceName ? (Array.from(sourceName)[0]?.toLocaleUpperCase() ?? '') : ''

  return {
    displayName,
    initial,
    backgroundColor: AVATAR_COLORS[stableColorIndex(displayName)],
  }
}
