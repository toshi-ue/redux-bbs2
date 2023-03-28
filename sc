#!/bin/bash

# dc=$(which docker compose) # docker-compose command with full path
dc="docker compose"

# if [[ -x "$dc" ]]; then
#   :
# else
#   echo "Please install Docker before run this command."
#   exit 2
# fi

rm="--rm" # To destroy a container

app="front" # describe $application service name from docker-compose.yml

db="db" # describe database service name from docker-compose.yml

app_name=$(pwd | awk -F "/" '{ print $NF }') # get project dir name

# define container name
app_container="${app_name}-${app}-1"

create_project() {
  webpack=""
  for arg in $@; do
    case $arg in
    --webpack*) webpack="true" ;;
    *) ;;
    esac
  done

  echoing "Exec Bundle Install for executing rails new command"
  bundle_cmd install

  echoing "Exec rails new with mysql and webpack"
  bundle_exec rails new . -f -d=mysql $*

  echoing "Exec Bundle Update for alerts"
  bundle_cmd update

  echoing "Update config/database.yml"
  mv database.yml config/database.yml

  echoing "Exec db create"
  bundle_exec rails db:create

  if [ "true" == "$webpack" ]; then
    echoing "Exec webpacker:install"
    bundle_exec rails webpacker:install
  fi

  echoing "docker-compose up"
  compose_up $app

  echo "You can access to localhost:3000"
}

init_services() {
  echoing "Building containers"
  $dc down -v
  $dc build --no-cache $app

  bundle_cmd install

  if [ "--webpack" == "$1" ]; then
    run_yarn install
  fi

  rails_cmd db:migrate:reset
  rails_cmd db:seed

  rm_pids

  $dc up $app
}

bundle_cmd() {
  run_app bundle $*
}

bundle_exec() {
  run_app bundle exec $*
}

bundle_exec_no_deps() {
  invoke_run_no_deps $app bundle exec $*
}

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
  # renv=""
  # if [ -n "$RAILS_ENV" ]; then
  #   renv="-e RAILS_ENV=$RAILS_ENV "
  # fi

  # if [ -n "$TRUNCATE_LOGS" ]; then
  #   renv="$renv -e TRUNCATE_LOGS=$TRUNCATE_LOGS "
  # fi

  # dbenv=""
  # if [ -n "$DISABLE_DATABASE_ENVIRONMENT_CHECK" ]; then
  #   dbenv="-e DISABLE_DATABASE_ENVIRONMENT_CHECK=$DISABLE_DATABASE_ENVIRONMENT_CHECK "
  # fi

  $dc run $rm ${renv}${dbenv}$*
}

invoke_run_no_deps() {
  # renv=""
  # if [ -n "$RAILS_ENV" ]; then
  #   renv="-e RAILS_ENV=$RAILS_ENV "
  # fi

  # if [ -n "$TRUNCATE_LOGS" ]; then
  #   renv="$renv -e TRUNCATE_LOGS=$TRUNCATE_LOGS "
  # fi

  # dbenv=""
  # if [ -n "$DISABLE_DATABASE_ENVIRONMENT_CHECK" ]; then
  #   dbenv="-e DISABLE_DATABASE_ENVIRONMENT_CHECK=$DISABLE_DATABASE_ENVIRONMENT_CHECK "
  # fi

  $dc run --no-deps $rm ${renv}${dbenv}$*
}

run_app() {
  invoke_run $app $*
}

run_db() {
  invoke_run $db $*
}

run_npm() {
  run_app npm $*
}

run_yarn() {
  run_app yarn $*
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
yarn)
  run_yarn $*
  ;;
*)
  read -d '' help <<-EOF
Usage: $0 command

Service:
  setup    Create new rails application
  init     Initialize backend services then run
  ps       Show status of services
  up       Create service containers and start backend services
  down     Stop backend services and remove service containers
  start    Start services
  stop     Stop services
  logs     [options] default: none. View output from containers
  bash     [service] invoke bash
  run      [service] [command] run command in given container

App:
  server   Run rails server
  rails    [args] Run rails command in application container
  rake     [args] Run rake command in application container
  db       [args] Run rails db command you can use set(migrate), up, down, reset, other is status
           ex: ./sc db set #running rails db:migrate
               ./sc db up 2019010101 #running rails db:migrate:up VERSION=2019010101
  rspec    [args] Run rspec command in application container
  test     [args] Run Minitest command in application container
  bundle   [args] Run bundle command in application container
  cons     Run rails console
  rubocop  [args] Run rubocop
  yarn      Run yarn command in application container
  npm       Run npm  command in application container

Spring
  spring    Exec spring command in Spring container
  sdive     Into spring container
  sdb       [args] Run rails db command you can use set(migrate), up, down, reset, other is status
             ex: ./sc db set #running rails db:migrate
                 ./sc db up 2019010101 #running rails db:migrate:up VERSION=2019010101

Solargraph
  solargraph Run solargraph command in Spring container

DB:
  reset-db  reset database in DB container
  psql      launch psql console in DB container
  pg-dump   dump database data as sql file in DB container
EOF
  echo "$help"
  exit 2
  ;;
esac
