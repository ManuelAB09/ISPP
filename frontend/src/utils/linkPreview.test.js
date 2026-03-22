import { extractFirstUrl } from './linkPreview';

describe('extractFirstUrl', () => {
  test('extracts http URL from text', () => {
    expect(extractFirstUrl('Check http://example.com for info')).toBe('http://example.com');
  });

  test('extracts https URL from text', () => {
    expect(extractFirstUrl('Visit https://google.com')).toBe('https://google.com');
  });

  test('normalizes www. prefix to https', () => {
    expect(extractFirstUrl('See www.example.com')).toBe('https://www.example.com');
  });

  test('returns null for text without URLs', () => {
    expect(extractFirstUrl('Just plain text')).toBeNull();
  });

  test('returns null for null input', () => {
    expect(extractFirstUrl(null)).toBeNull();
  });

  test('returns null for empty string', () => {
    expect(extractFirstUrl('')).toBeNull();
  });

  test('returns null for non-string input', () => {
    expect(extractFirstUrl(123)).toBeNull();
  });

  test('strips trailing punctuation', () => {
    const result = extractFirstUrl('Go to https://example.com.');
    expect(result).toBe('https://example.com');
  });

  test('extracts first URL when multiple present', () => {
    const result = extractFirstUrl('Visit https://first.com and https://second.com');
    expect(result).toBe('https://first.com');
  });
});
