use env_logger::Env;
use std::io::Write;

pub fn init() {
    let env = Env::default().filter_or("LOG_LEVEL", "info");

    env_logger::Builder::from_env(env)
        .format(|buf, record| {
            writeln!(
                buf,
                "<{}> - [{}] {} - {}",
                chrono::Local::now().format("%Y-%m-%d %H:%M:%S"),
                record.target(),
                record.level(),
                record.args()
            )
        })
        .filter_level(log::LevelFilter::Info)
        .init();
}
