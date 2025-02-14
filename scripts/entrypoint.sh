#!/bin/bash

if [ -d "/app/TestEval/out" ]; then
    cp -r /app/TestEval/out/* /app/volume/
fi

# Continue with the default command
exec "$@"