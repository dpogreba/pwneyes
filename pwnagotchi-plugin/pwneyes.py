# pwneyes.py — exposes GET /plugins/pwneyes/status for the PwnEyes Android app.
#
# Install: copy to your Pwnagotchi custom-plugins dir (default
#   /usr/local/share/pwnagotchi/custom-plugins/), then in /etc/pwnagotchi/config.toml:
#       main.plugins.pwneyes.enabled = true
#   (optional overrides if your image differs:)
#       main.plugins.pwneyes.handshake_dir = "/root/handshakes"
#       main.plugins.pwneyes.potfile = "/root/handshakes/wpa-sec.cracked.potfile"
#   then: sudo systemctl restart pwnagotchi  (or toggle from the web UI Plugins page).
#
# Endpoint: http://<device>:8080/plugins/pwneyes/status  (inherits the web UI's Basic Auth)
#   -> {"handshakes": <int>, "potfile_mtime": <epoch int|null>, "cracked": <int>}
import os
import logging
import pwnagotchi.plugins as plugins
from flask import jsonify, abort


class PwnEyes(plugins.Plugin):
    __author__ = 'PwnEyes'
    __version__ = '1.0.0'
    __license__ = 'MIT'
    __description__ = 'Exposes handshake + wpa-sec cracked counts for the PwnEyes app.'

    def on_loaded(self):
        self.handshake_dir = self.options.get('handshake_dir', '/root/handshakes')
        self.potfile = self.options.get(
            'potfile', os.path.join(self.handshake_dir, 'wpa-sec.cracked.potfile'))
        logging.info("[pwneyes] loaded; handshakes=%s potfile=%s",
                     self.handshake_dir, self.potfile)

    def on_webhook(self, path, request):
        if path != 'status':
            abort(404)

        try:
            handshakes = sum(1 for f in os.listdir(self.handshake_dir)
                             if f.endswith('.pcap'))
        except OSError:
            handshakes = 0

        cracked = 0
        potfile_mtime = None
        if os.path.isfile(self.potfile):
            try:
                potfile_mtime = int(os.path.getmtime(self.potfile))
                with open(self.potfile, 'r', errors='ignore') as fh:
                    cracked = sum(1 for line in fh if line.strip())
            except OSError:
                pass

        return jsonify({
            'handshakes': handshakes,
            'potfile_mtime': potfile_mtime,
            'cracked': cracked,
        })
