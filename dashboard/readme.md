Rename this app to screens

- software for screening securities
- platrofm to render aesome screens

#### one journey to solve svelte, *kit, vite deps probs

```
npm i -D @sveltejs/adapter-node
# cant do, beging updating evertything in the order of error
npm install -D svelte@latest
npm install -D svelte-check@latest
npm install -D @sveltejs/kit@latest
npm install -D vite@latest
# vite cannot be updated..
# identified that eslint for svelte requires an older version no longer supported by those others
# removed eslint
# redo npm install -D from above and then..
npm install -D svelte-preprocess
rm -rf node_modules package-lock.json && npm install
npx svelte-check
npm install typescript@latest --save-dev
# all the fixes remain at project files so normal deployemnt will work
```

```
npm run build
npm ci --omit dev
node -r dotenv/config build
```

CloudFront deployment
```
npm run build
aws s3 sync build/ s3://<your frontend S3 bucket> --delete
```
