const { exec } = require('child_process');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const gradlew = path.join(projectRoot, 'gradlew.bat');

console.log('Starting Android build process...');

const buildProcess = exec(`"${gradlew}" assembleDebug`, {
  cwd: projectRoot,
  env: {
    ...process.env,
    JAVA_HOME: 'C:/Program Files/Java/jdk-22',
    JWT_SECRET: '89VZJuRkKB0sglml',
    DB_URL: 'jdbc:postgresql://localhost:5432/contract_management',
    DB_DRIVER: 'org.postgresql.Driver',
    DB_USER: 'postgres',
    DB_PASSWORD: '235689'
  }
});

buildProcess.stdout.on('data', (data) => {
  console.log(data.toString());
});

buildProcess.stderr.on('data', (data) => {
  console.error(data.toString());
});

buildProcess.on('close', (code) => {
  console.log(`Build process exited with code ${code}`);
});