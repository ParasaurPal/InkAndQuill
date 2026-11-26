const CACHE="ink-quill-shell-v2";
const FILES=["/","/manifest.webmanifest"];
self.addEventListener("install",event=>event.waitUntil((async()=>{const cache=await caches.open(CACHE);const response=await fetch("/");await cache.put("/",response.clone());const html=await response.text();const assets=[...html.matchAll(/(?:src|href)="([^"]+\.(?:js|css|webmanifest))"/g)].map(match=>match[1]);await cache.addAll([...FILES,...assets]);await self.skipWaiting()})()));
self.addEventListener("activate",event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key!==CACHE).map(key=>caches.delete(key)))).then(()=>self.clients.claim())));
self.addEventListener("fetch",event=>{if(event.request.method!=="GET")return;event.respondWith(caches.match(event.request).then(hit=>hit||fetch(event.request).then(response=>{if(response.ok){const copy=response.clone();caches.open(CACHE).then(cache=>cache.put(event.request,copy))}return response}).catch(()=>caches.match("/"))))});
