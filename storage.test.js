import test from 'node:test';
import assert from 'node:assert/strict';
import { isSupportedProfile } from '../js/modules/performance.js';

test('known performance profiles are accepted', () => {
  assert.equal(isSupportedProfile('balanced'), true);
  assert.equal(isSupportedProfile('unknown'), false);
});
