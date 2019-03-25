module.exports = {
  root: true,
  'extends': [
    'plugin:vue/essential',
    '@vue/standard'
  ],
  rules: {
    // allow async-await
    'generator-star-spacing': 'off',
    // allow debugger during development
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'vue/no-parsing-error': [2, {
      'x-invalid-end-tag': false
    }],
    'no-undef': 'off',
    'camelcase': 'off',
    'no-extra-semi': 'off',
    'semi': 'off',
    'space-before-function-paren': 'off',
    'new-cap': 'off',
    'quotes': 'off'
  },
  parserOptions: {
    parser: 'babel-eslint'
  }
}
