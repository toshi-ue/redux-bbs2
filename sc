#!/bin/bash

# dc=$(which docker compose) # docker-compose command with full path

# if [[ -x "$dc" ]]; then
#   :
# else
#   echo "Please install Docker before run this command."
#   exit 2
# fi
dc="docker compose"

rm="--rm" # To destroy a container

app="front" # describe $application service name from docker-compose.yml

db="db" # describe database service name from docker-compose.yml

app_name=$(pwd | awk -F "/" '{ print $NF }') # get project dir name

# define container name
app_container="${app_name}-${app}-1"

compose_build() {
  echoing "Build containers $*"
  $dc build $*
}

compose_down() {
  echoing "Stop and remove containers $*"
  $dc down $*
}

compose_ps() {
  echoing "Showing running containers"
  $dc ps
}

compose_restart() {
  echoing "Restart services $*"
  $dc restart $*
}

compose_start() {
  echoing "Start services $*"
  rm_pids
  $dc start $*
}

compose_stop() {
  echoing "Stop services $*"
  $dc stop $*
}

compose_up() {
  echoing "Create and start containers $*"
  # rm_pids
  $dc up -d
  # docker attach $app_container
  $dc logs -f
}

echoing() {
  echo "========================================================"
  echo "$1"
  echo "========================================================"
}

logs() {
  echoing "Logs $*"
  $dc logs -f $1
}

invoke_bash() {
  $dc run $rm -u root $1 bash
}

invoke_run() {
  $dc run $rm "$@"
}

run_app() {
  invoke_run $app "$@"
}

run_db() {
  invoke_run $db $*
}

run_npm() {
  run_app sh -c "cd front && npm $*"
}

cmd=$1
shift
case "$cmd" in
bash)
  invoke_bash $*
  ;;
build)
  compose_build $* && exit 0
  ;;
db-dump)
  db_dump $*
  ;;
down)
  compose_down $* && exit 0
  ;;
enchoing)
  enchoing
  ;;
init)
  init_services $* && exit 0
  ;;
logs)
  logs $*
  ;;
npm)
  run_npm $*
  ;;
ps)
  compose_ps && exit 0
  ;;
restart)
  compose_restart $* && exit 0
  ;;
run)
  invoke_run $*
  ;;
setup)
  create_project $* && exit 0
  ;;
start)
  compose_start $* && exit 0
  ;;
stop)
  compose_stop $* && exit 0
  ;;
up)
  # compose_up $* && compose_ps && exit 0
  compose_up $* && exit 0
  ;;
*)
  read -d '' help <<-EOF
Usage: $0 command

Service:
  ps       Show status of services
  up       Create service containers and start backend services
  down     Stop backend services and remove service containers
  start    Start services
  stop     Stop services
  logs     [options] default: none. View output from containers
  bash     [service] invoke bash
  run      [service] [command] run command in given container

App:
  npm       Run npm  command in application container

DB:
  reset-db  reset database in DB container
  psql      launch psql console in DB container
  pg-dump   dump database data as sql file in DB container
EOF
  echo "$help"
  exit 2
  ;;
esac
