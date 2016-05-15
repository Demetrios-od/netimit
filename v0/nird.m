% Script for reading data saved by NETIMIT program
% (NIRD = NETIMIT reading data)

clear

% For instructions, see the book, page 57.
f=fopen('res.dat','rb');  % opening a binary file for reading

fseek(f,0,1);   % go to the EOF
N=ftell(f)/96;  % one record has 96 bytes - 56 for initial data and 40 for results
if (N-fix(N)) ~= 0
    error('Incorrect size of file.');
end
frewind(f);     % go to the begin

for i=1:N
    % reading the initial data
    d(i).num_abons=fread(f,1,'uint16');
    d(i).NBS=fread(f,1,'uint16');
    d(i).calldur=fread(f,2,'uint16');
    d(i).Ncar=fread(f,1,'uint16');
    d(i).Ncpc=fread(f,1,'uint16');
    d(i).maxiters=fread(f,1,'uint32');
    d(i).betta=fread(f,1,'float32');
    d(i).speed=fread(f,2,'float32');
    d(i).area=fread(f,4,'float32');
    d(i).covzone=fread(f,2,'float32');
    d(i).Rad=fread(f,1,'float32');
    r(i).MSG=fread(f,10,'uint32');
end

fclose(f);
clear f i N ans
disp('Data has been readed from the file.')

% Now all variables are in corresponding arrays,
% and we can process that data.
