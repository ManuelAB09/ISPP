import getIdFromUrl from './getIdFromUrl';

describe('getIdFromUrl', () => {
  const originalLocation = window.location;

  afterEach(() => {
    delete window.location;
    window.location = originalLocation;
  });

  function setPathname(path) {
    delete window.location;
    window.location = { ...originalLocation, pathname: path };
  }

  test('extracts id at index 2 from /community/123/chat', () => {
    setPathname('/community/123/chat');
    expect(getIdFromUrl(2)).toBe('123');
  });

  test('extracts segment at index 1', () => {
    setPathname('/tutores/42');
    expect(getIdFromUrl(1)).toBe('tutores');
  });

  test('returns undefined for out-of-range index', () => {
    setPathname('/a/b');
    expect(getIdFromUrl(10)).toBeUndefined();
  });

  test('returns empty string for index 0 with leading slash', () => {
    setPathname('/page');
    expect(getIdFromUrl(0)).toBe('');
  });
});
