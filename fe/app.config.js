// google-services.json은 gitignore라 EAS 빌드 서버에 올라가지 않음 → EAS 파일 환경변수(GOOGLE_SERVICES_JSON) 경로를 씀
module.exports = ({ config }) => ({
  ...config,
  android: {
    ...config.android,
    googleServicesFile: process.env.GOOGLE_SERVICES_JSON ?? config.android.googleServicesFile,
  },
});
