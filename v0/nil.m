% Script for launching the NETIMIT program.
% Contains setting up the initial data and processing results.
%
% (C) Bukhan D.Yu. 2012-2013

%-------------------- Setting up the initial data --------------------

% Abonents
num_abons=400;   % quantity of abons
betta=0.02;      % probability of call appearance at iteration
               % (if multiplied by call duration, gives load
               % created by single abon, in Erlangs)
srvarea.xmin=1800;        % coordinates of the area where abons
srvarea.xmax=7700;        %   can move, in meters
srvarea.ymin=1100;
srvarea.ymax=6400;
%maxspeed=130;
speed.aver=100;  % average speed, meters per iteration (minute)
speed.disp=30;   %   100 m/min = 6 km/h
% Base stations
NBS=16;    % number of BS
Rad=1000;  % cell radius, meters
covzone.lbcX=0;    % coordinates of the left and bottom corner
covzone.lbcY=0;    % of the coverage zone

% Other important parameters
maxiters=1440;        % quantity of iterations in modeling
pause_duration=0.2;     % pause between iterations, sec
calldur.aver=14;  % average call duration, iterations (minutes)
calldur.disp=3;   % dispersion of call duration, iterations (minutes)
Ncar=9;       % number of carriers
Ncpc=7;       % number of traffic channels per carrier

% Visualization parameters
vis.showiter=false;   % show iteration number
vis.everyiter=100;    % every this iteration will be reported
vis.showmes=false;   % show info messages
vis.shownet=false;   % visualize the cellular network and abons' moving,
                     %   doing pause
vis.showbar=true;    % show progress bar

%------------------------- Launch modeling ----------------------------

[MSG,CNT]=netimit(num_abons, betta, srvarea, speed, NBS, Rad, covzone,...
                  maxiters, pause_duration, calldur, Ncar, Ncpc, vis);

%--------------------- Processing the results --------------------------

p=(MSG.CALL_REQ_FAIL + MSG.CALL_REL_HAND)*100/...
    (MSG.CALL_REQ_FAIL + MSG.CALL_REQ_SUC);

MSG
disp(strcat('Probability of call loss: p=',num2str(p),'%'))
