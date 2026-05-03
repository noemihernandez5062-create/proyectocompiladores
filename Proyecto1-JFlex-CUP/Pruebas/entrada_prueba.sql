create table estudiante (
    edad int,
    nombre varchar(50),
    fecha_nacimiento datetime,
    score decimal(18,2)
);

select conteo(*) from tb_estudiante;

select * from tabla1 a join tabla2 b on (b.id = a.id);

update tabla1 set campo = '123' where condicion = 123;

insert into tabla1(campo1, campo2) values (123, 'Juan');

insert into tabla1 select * from tabla2;

insert into tabla1(campo1, campo2) select a.campo1, a.campo2 from tabla2 a join tabla3 b on (b.id = a.id);