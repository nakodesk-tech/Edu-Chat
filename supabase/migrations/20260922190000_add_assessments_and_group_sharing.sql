-- Assessment module: creation, group sharing, student attempts, and RLS
-- Applied to EduChat project as add_assessments_and_group_sharing.

create table if not exists public.assessments (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  subject text not null,
  standard text not null,
  duration_minutes integer not null check (duration_minutes between 1 and 300),
  status text not null default 'draft' check (status in ('draft','published','scheduled')),
  total_questions integer not null default 0 check (total_questions >= 0),
  created_by uuid not null references public.profiles(id),
  school_id uuid references public.schools(id),
  created_at timestamptz not null default timezone('utc', now()),
  updated_at timestamptz not null default timezone('utc', now()),
  published_at timestamptz
);

create table if not exists public.assessment_questions (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.assessments(id) on delete cascade,
  question_no integer not null check (question_no > 0),
  question_text text not null,
  options jsonb not null,
  correct_option text not null check (correct_option in ('A','B','C','D')),
  marks integer not null default 1 check (marks > 0),
  created_at timestamptz not null default timezone('utc', now()),
  unique (assessment_id, question_no)
);

create table if not exists public.assessment_group_shares (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.assessments(id) on delete cascade,
  group_id uuid not null references public.groups(id) on delete cascade,
  shared_by uuid not null references public.profiles(id),
  shared_at timestamptz not null default timezone('utc', now()),
  unique (assessment_id, group_id)
);

create table if not exists public.assessment_attempts (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.assessments(id) on delete cascade,
  student_id uuid not null references public.profiles(id),
  group_id uuid not null references public.groups(id),
  answers jsonb not null default '{}'::jsonb,
  score integer not null default 0,
  total_marks integer not null default 0,
  status text not null default 'completed' check (status in ('completed')),
  started_at timestamptz not null default timezone('utc', now()),
  completed_at timestamptz not null default timezone('utc', now()),
  unique (assessment_id, student_id, group_id)
);

create index if not exists assessments_created_by_idx on public.assessments(created_by);
create index if not exists assessments_school_id_idx on public.assessments(school_id);
create index if not exists assessment_questions_assessment_id_idx on public.assessment_questions(assessment_id);
create index if not exists assessment_group_shares_group_id_idx on public.assessment_group_shares(group_id);
create index if not exists assessment_attempts_student_id_idx on public.assessment_attempts(student_id);
create index if not exists assessment_attempts_assessment_id_idx on public.assessment_attempts(assessment_id);

alter table public.assessments enable row level security;
alter table public.assessment_questions enable row level security;
alter table public.assessment_group_shares enable row level security;
alter table public.assessment_attempts enable row level security;

revoke all on public.assessments, public.assessment_questions, public.assessment_group_shares, public.assessment_attempts from anon;
grant select on public.assessments, public.assessment_questions, public.assessment_group_shares, public.assessment_attempts to authenticated;

create policy "assessments_select_authorized" on public.assessments for select to authenticated using (
  created_by = auth.uid() or public.current_user_role() = 'officer_admin'
  or (public.current_user_role() in ('school_admin','teacher') and school_id = public.current_user_school_id())
  or exists (
    select 1 from public.assessment_group_shares s
    join public.group_members gm on gm.group_id = s.group_id
    where s.assessment_id = assessments.id and gm.user_id = auth.uid() and gm.is_active = true
  )
);

create policy "assessment_questions_select_authorized" on public.assessment_questions for select to authenticated using (
  exists (
    select 1 from public.assessments a
    where a.id = assessment_questions.assessment_id
      and (
        a.created_by = auth.uid() or public.current_user_role() = 'officer_admin'
        or (public.current_user_role() in ('school_admin','teacher') and a.school_id = public.current_user_school_id())
        or exists (
          select 1 from public.assessment_group_shares s
          join public.group_members gm on gm.group_id = s.group_id
          where s.assessment_id = a.id and gm.user_id = auth.uid() and gm.is_active = true
        )
      )
  )
);

create policy "assessment_group_shares_select_authorized" on public.assessment_group_shares for select to authenticated using (
  shared_by = auth.uid() or public.current_user_role() = 'officer_admin'
  or exists (
    select 1 from public.group_members gm
    where gm.group_id = assessment_group_shares.group_id and gm.user_id = auth.uid() and gm.is_active = true
  )
);

create policy "assessment_attempts_select_authorized" on public.assessment_attempts for select to authenticated using (
  student_id = auth.uid() or public.current_user_role() = 'officer_admin'
  or exists (
    select 1 from public.assessments a
    where a.id = assessment_attempts.assessment_id
      and (
        a.created_by = auth.uid()
        or (public.current_user_role() in ('school_admin','teacher') and a.school_id = public.current_user_school_id())
      )
  )
);

revoke insert, update, delete on public.assessments, public.assessment_questions, public.assessment_group_shares, public.assessment_attempts from authenticated;

create or replace function public.create_assessment(p_title text,p_subject text,p_standard text,p_duration_minutes integer,p_questions jsonb,p_status text default 'draft')
returns public.assessments language plpgsql security definer set search_path = '' as $$
declare v_profile public.profiles%rowtype; v_assessment public.assessments%rowtype;
begin
  select * into v_profile from public.profiles where id=auth.uid() and is_active=true;
  if v_profile.id is null or v_profile.role not in ('officer_admin','school_admin','teacher') then raise exception 'Assessment creation is not authorized'; end if;
  if nullif(trim(p_title),'') is null or nullif(trim(p_subject),'') is null or nullif(trim(p_standard),'') is null then raise exception 'Title, subject and standard are required'; end if;
  if p_duration_minutes is null or p_duration_minutes < 1 or p_duration_minutes > 300 then raise exception 'Duration must be between 1 and 300 minutes'; end if;
  if jsonb_typeof(p_questions) <> 'array' or jsonb_array_length(p_questions) < 1 then raise exception 'At least one question is required'; end if;
  if p_status not in ('draft','published','scheduled') then raise exception 'Invalid assessment status'; end if;
  insert into public.assessments(title,subject,standard,duration_minutes,status,total_questions,created_by,school_id,published_at)
  values(trim(p_title),trim(p_subject),trim(p_standard),p_duration_minutes,p_status,jsonb_array_length(p_questions),auth.uid(),case when v_profile.role='officer_admin' then null else v_profile.school_id end,case when p_status='published' then timezone('utc',now()) else null end)
  returning * into v_assessment;
  insert into public.assessment_questions(assessment_id,question_no,question_text,options,correct_option,marks)
  select v_assessment.id,q.question_no,q.question_text,q.options,q.correct_option,coalesce(q.marks,1)
  from jsonb_to_recordset(p_questions) as q(question_no integer,question_text text,options jsonb,correct_option text,marks integer);
  if exists(select 1 from public.assessment_questions where assessment_id=v_assessment.id and (nullif(trim(question_text),'') is null or jsonb_typeof(options)<>'object' or correct_option not in ('A','B','C','D'))) then raise exception 'Invalid question data'; end if;
  return v_assessment;
end; $$;

create or replace function public.share_assessment_to_group(p_assessment_id uuid,p_group_id uuid)
returns public.assessment_group_shares language plpgsql security definer set search_path = '' as $$
declare v_profile public.profiles%rowtype; v_assessment public.assessments%rowtype; v_group public.groups%rowtype; v_share public.assessment_group_shares%rowtype;
begin
  select * into v_profile from public.profiles where id=auth.uid() and is_active=true;
  if v_profile.id is null or v_profile.role not in ('officer_admin','school_admin','teacher') then raise exception 'Assessment sharing is not authorized'; end if;
  select * into v_assessment from public.assessments where id=p_assessment_id;
  select * into v_group from public.groups where id=p_group_id and is_active=true;
  if v_assessment.id is null or v_group.id is null then raise exception 'Assessment or group not found'; end if;
  if v_assessment.created_by<>auth.uid() and v_profile.role<>'officer_admin' then raise exception 'Only the creator or Officer Admin can share this assessment'; end if;
  if v_profile.role<>'officer_admin' and v_group.school_id is distinct from v_profile.school_id then raise exception 'Group is outside the current school'; end if;
  insert into public.assessment_group_shares(assessment_id,group_id,shared_by) values(p_assessment_id,p_group_id,auth.uid())
  on conflict(assessment_id,group_id) do update set shared_by=excluded.shared_by,shared_at=timezone('utc',now()) returning * into v_share;
  return v_share;
end; $$;

create or replace function public.submit_assessment_attempt(p_assessment_id uuid,p_group_id uuid,p_answers jsonb)
returns public.assessment_attempts language plpgsql security definer set search_path = '' as $$
declare v_assessment public.assessments%rowtype; v_total integer; v_score integer; v_attempt public.assessment_attempts%rowtype;
begin
  select * into v_assessment from public.assessments a where a.id=p_assessment_id and exists(
    select 1 from public.assessment_group_shares s join public.group_members gm on gm.group_id=s.group_id
    where s.assessment_id=a.id and s.group_id=p_group_id and gm.user_id=auth.uid() and gm.is_active=true);
  if v_assessment.id is null then raise exception 'Assessment is not shared with this student group'; end if;
  if not exists(select 1 from public.profiles p where p.id=auth.uid() and p.role='student' and p.is_active=true) then raise exception 'Only active students can submit an assessment'; end if;
  select coalesce(sum(marks),0) into v_total from public.assessment_questions where assessment_id=p_assessment_id;
  select coalesce(sum(q.marks),0) into v_score from public.assessment_questions q join jsonb_each_text(coalesce(p_answers,'{}'::jsonb)) ans on ans.key=q.question_no::text where q.assessment_id=p_assessment_id and upper(ans.value)=upper(q.correct_option);
  insert into public.assessment_attempts(assessment_id,student_id,group_id,answers,score,total_marks) values(p_assessment_id,auth.uid(),p_group_id,coalesce(p_answers,'{}'::jsonb),v_score,v_total)
  on conflict(assessment_id,student_id,group_id) do update set answers=excluded.answers,score=excluded.score,total_marks=excluded.total_marks,completed_at=timezone('utc',now()),status='completed'
  returning * into v_attempt;
  return v_attempt;
end; $$;

revoke execute on function public.create_assessment(text,text,text,integer,jsonb,text),public.share_assessment_to_group(uuid,uuid),public.submit_assessment_attempt(uuid,uuid,jsonb) from public,anon;
grant execute on function public.create_assessment(text,text,text,integer,jsonb,text),public.share_assessment_to_group(uuid,uuid),public.submit_assessment_attempt(uuid,uuid,jsonb) to authenticated;
