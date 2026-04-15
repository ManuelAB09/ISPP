jest.mock('../api/baseUrl', () => ({
  getApiBaseUrl: () => 'https://api.test',
}));

const {
  resolveCommunityImage,
  resolveUserImage,
  DEFAULT_COMMUNITY_IMAGE,
  DEFAULT_PROFILE_AVATAR,
} = require('./imageUtils');

describe('imageUtils', () => {
  test('resolveCommunityImage returns default when value is missing', () => {
    expect(resolveCommunityImage(null)).toBe(DEFAULT_COMMUNITY_IMAGE);
    expect(resolveCommunityImage({ imagen: '   ' })).toBe(DEFAULT_COMMUNITY_IMAGE);
  });

  test('resolveCommunityImage returns default for placeholder string values', () => {
    expect(resolveCommunityImage({ imagen: 'empty' })).toBe(DEFAULT_COMMUNITY_IMAGE);
    expect(resolveCommunityImage({ imagenUrl: 'null' })).toBe(DEFAULT_COMMUNITY_IMAGE);
    expect(resolveCommunityImage({ foto: 'undefined' })).toBe(DEFAULT_COMMUNITY_IMAGE);
  });

  test('resolveCommunityImage keeps absolute and data/blob URLs', () => {
    expect(resolveCommunityImage({ imagen: 'https://cdn.example.com/img.png' })).toBe('https://cdn.example.com/img.png');
    expect(resolveCommunityImage({ imagen: 'data:image/png;base64,abc' })).toBe('data:image/png;base64,abc');
    expect(resolveCommunityImage({ imagen: 'blob:http://localhost/id-1' })).toBe('blob:http://localhost/id-1');
  });

  test('resolveCommunityImage converts relative URLs using API base URL', () => {
    expect(resolveCommunityImage({ imagen: '/uploads/community.png' })).toBe('https://api.test/uploads/community.png');
    expect(resolveCommunityImage({ imagen: 'uploads/community.png' })).toBe('https://api.test/uploads/community.png');
  });

  test('resolveUserImage returns default when value is missing or invalid', () => {
    expect(resolveUserImage('')).toBe(DEFAULT_PROFILE_AVATAR);
    expect(resolveUserImage('null')).toBe(DEFAULT_PROFILE_AVATAR);
    expect(resolveUserImage('undefined')).toBe(DEFAULT_PROFILE_AVATAR);
  });

  test('resolveUserImage keeps absolute URL and resolves relative path', () => {
    expect(resolveUserImage('https://cdn.example.com/avatar.png')).toBe('https://cdn.example.com/avatar.png');
    expect(resolveUserImage('/avatars/user.png')).toBe('https://api.test/avatars/user.png');
    expect(resolveUserImage('avatars/user.png')).toBe('https://api.test/avatars/user.png');
  });
});
